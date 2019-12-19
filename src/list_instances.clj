(ns list-instances
  (:require [cognitect.aws.client.api :as aws]))

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

(defn -main [& args]
  (do
    (doseq [instance (instances)]
      (println instance))
    (System/exit 0)))