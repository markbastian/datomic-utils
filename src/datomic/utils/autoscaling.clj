(ns datomic.utils.autoscaling
  (:require [cognitect.aws.client.api :as aws]
            [clojure.string :as cs]))

(defonce autoscaling (aws/client {:api :autoscaling}))
;(defonce autoscaling-plans (aws/client {:api :autoscaling-plans}))

(defn describe-autoscaling-groups [system-name]
  (let [{:keys [AutoScalingGroups]} (aws/invoke autoscaling {:op :DescribeAutoScalingGroups})]
    (for [{:keys [Tags] :as AutoScalingGroup} AutoScalingGroups
          {:keys [Key Value]} Tags
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