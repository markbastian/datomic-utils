(ns stacks
  (:require [cognitect.aws.client.api :as aws]))

(defonce cf (aws/client {:api :cloudformation}))

(defn list []
  (aws/invoke cf {:op :ListStacks}))

;Goal - automate this
;https://docs.datomic.com/cloud/operation/new-system.html

;https://s3.amazonaws.com/datomic-cloud-1/cft/569-8835/datomic-solo-compute-569-8835.json

;This doesn't work yet. What's the issue?
(defn create-compute [system-name key-name]
  (aws/invoke
    cf
    {:op      :CreateStack
     :request {:Parameters   [{:ParameterKey "SystemName" :ParameterValue system-name}
                              {:ParameterKey "ApplicationName" :ParameterValue system-name}
                              {:ParameterKey "KeyName" :ParameterValue key-name}]
               :Capabilities ["CAPABILITY_NAMED_IAM"]
               :StackName    system-name
               :TemplateURL  "https://s3.amazonaws.com/datomic-cloud-1/cft/569-8835/datomic-solo-compute-569-8835.json"}}))

(defn -main [& args]
  (do
    ;Just a big dump right now.
    (println (list))
    (System/exit 0)))
