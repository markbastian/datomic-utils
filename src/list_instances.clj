(ns list-instances
  (:require [cognitect.aws.client.api :as aws]
            [clojure.string :as cs]))

(defonce ec2 (aws/client {:api :ec2}))
(defonce iam (aws/client {:api :iam}))
;(defonce application-autoscaling (aws/client {:api :application-autoscaling}))
(defonce autoscaling (aws/client {:api :autoscaling}))
;(defonce autoscaling-plans (aws/client {:api :autoscaling-plans}))

(defn describe-autoscaling-groups [system-name]
  (let [{:keys [AutoScalingGroups]} (aws/invoke autoscaling {:op :DescribeAutoScalingGroups})]
    (for [{:keys [Tags] :as AutoScalingGroup} AutoScalingGroups
          {:keys [Key Value ResourceType]} Tags
          :when (and (= Key "datomic:system") (= Value system-name))]
      AutoScalingGroup)))

(defn bastion? [{:keys [Tags] :as asg}]
  (letfn [(b? [{:keys [Key Value ResourceType]}]
            (when (and (= ResourceType "auto-scaling-group") (= Key "Name"))
              [(cs/ends-with? Value "-bastion")]))]
    (->> Tags (some b?) first)))

(defn set-running-capacities [system-name]
  (let [asgs (describe-autoscaling-groups system-name)]
    (for [asg asgs]
      (-> asg
          (assoc :DesiredCapacity 1)
          (cond-> (not (bastion? asg)) (assoc :MinSize 1))
          (assoc :MaxSize 1)))))

(defn set-stopped-capacities [system-name]
  (let [asgs (describe-autoscaling-groups system-name)]
    (for [asg asgs]
      (-> asg
          (assoc :DesiredCapacity 0)
          (assoc :MinSize 0)
          (assoc :MaxSize 0)))))

(defn startup-datomic-system [system-name]
  (->> system-name
       set-running-capacities
       (mapv #(aws/invoke autoscaling {:op :UpdateAutoScalingGroup :request %}))))

(defn shutdown-datomic-system [system-name]
  (->> system-name
       set-stopped-capacities
       (mapv #(aws/invoke autoscaling {:op :UpdateAutoScalingGroup :request %}))))

(comment
  (describe-autoscaling-groups "repl-ion")
  (startup-datomic-system "repl-ion")
  (shutdown-datomic-system "repl-ion"))

(defn users []
  (let [{:keys [Users]} (aws/invoke iam {:op :ListUsers})]
    (map :UserName Users)))

(def query {:Filters [{:Name "tag-key" :Values ["datomic:tx-group"]}
                      {:Name "instance-state-name" :Values ["running"]}]})

(defn instances []
  (let [{:keys [Reservations]} (aws/invoke ec2 {:op :DescribeInstances :request query})]
    (for [{:keys [Instances]} Reservations
          {:keys [Tags]} Instances
          {:keys [Key Value]} Tags
          :when (= Key "datomic:system")]
      Value)))

;https://github.com/cognitect-labs/aws-api/blob/master/latest-releases.edn
;Potential for replion
;:UpdateSecurityGroupRuleDescriptionsEgress
;:UpdateSecurityGroupRuleDescriptionsIngress

(defn -main [& args]
  (do
    (doseq [instance (instances)]
      (println instance))
    (System/exit 0)))