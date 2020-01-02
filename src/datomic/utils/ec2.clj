(ns datomic.utils.ec2
  (:require [cognitect.aws.client.api :as aws]
            [clojure.string :as cs]))

(defonce ec2 (aws/client {:api :ec2}))

(def query {:Filters [{:Name "tag-key" :Values ["datomic:tx-group"]}
                      {:Name "instance-state-name" :Values ["running"]}]})

(defn instances []
  (let [{:keys [Reservations]} (aws/invoke ec2 {:op :DescribeInstances :request query})]
    (for [{:keys [Instances]} Reservations
          {:keys [Tags]} Instances
          {:keys [Key Value]} Tags
          :when (= Key "datomic:system")]
      Value)))

(defn security-group [system-name]
  (let [{:keys [Reservations]} (aws/invoke ec2 {:op :DescribeInstances :request query})]
    (first
      (for [{:keys [Instances]} Reservations
            {:keys [Tags SecurityGroups]} Instances
            {:keys [Key Value]} Tags
            :when (and
                    (= Key "datomic:system")
                    (= Value system-name))
            SecurityGroup SecurityGroups]
        SecurityGroup))))

(defn security-group-details [system-name]
  (let [{:keys [GroupName GroupId]} (security-group system-name)]
    (->
      (aws/invoke
        ec2
        {:op      :DescribeSecurityGroups
         :request {:Filters [{:Name "group-name" :Values [GroupName]}
                             {:Name "group-id" :Values [GroupId]}]}})
      :SecurityGroups
      first)))

(defn make-system-replable [system-name]
  (let [{[f :as IpPermissions] :IpPermissions :keys [GroupId]} (security-group-details system-name)
        mods (-> f
                 (update :IpRanges (fn [s] (mapv #(update % :Description (fn [s] (cs/replace s "CIDRBlock" "REPL port"))) s)))
                 (assoc :FromPort 3001)
                 (assoc :ToPort 3001))]
    #_(aws/invoke
        ec2
        {:op      :UpdateSecurityGroupRuleDescriptionsIngress
         :request {:GroupId       GroupId
                   ;:DryRun true
                   :IpPermissions [f mods]}})
    {:GroupId       GroupId
     ;:DryRun true
     :IpPermissions [f mods]}))