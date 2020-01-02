(ns datomic.utils.create-datomic
  (:require [cognitect.aws.client.api :as aws]
            [hickory.core :as hik]
            [hickory.select :as s]
            [clojure.string :as cs]
            [generic.utils.ec2 :refer [keypairs]])
  (:import (java.util Date)))

(defonce cf (aws/client {:api :cloudformation}))

(defn stack-status [stack-name]
  (let [{:keys [StackSummaries]} (aws/invoke cf {:op :ListStacks})]
    (some->> StackSummaries
             (filter (comp #{stack-name} :StackName))
             seq
             (apply max-key (fn [{:keys [^Date CreationTime]}] (.getTime CreationTime))))))

(defn stack-status-1 [{:keys [StackId]}]
  (let [{:keys [StackSummaries]} (aws/invoke cf {:op :ListStacks})]
    (some->> StackSummaries
             (filter (comp #{StackId} :StackId))
             seq
             (apply max-key (fn [{:keys [^Date CreationTime]}] (.getTime CreationTime))))))

(defn latest-datomic-cloudformation-templates []
  (let [h (-> "https://docs.datomic.com/cloud/releases.html#release-history"
              slurp
              hik/parse
              hik/as-hickory)
        table (s/select
                (s/child
                  (s/id "text-release-history")
                  (s/tag :table)
                  (s/tag :tbody)
                  (s/tag :tr)
                  (s/tag :td)
                  (s/tag :code)
                  (s/attr :data-clipboard-text))
                h)
        templates (map (comp :data-clipboard-text :attrs) table)
        [storage-template :as storage-templates] (filter #(cs/includes? % "datomic-storage-") templates)
        [solo-template :as solo-compute-templates] (filter #(cs/includes? % "datomic-solo-compute-") templates)
        [production-template :as production-templates] (filter #(cs/includes? % "datomic-production-compute-") templates)
        [query-group-template :as query-group-templates] (filter #(cs/includes? % "datomic-query-group-") templates)]
    {:storage     storage-template
     :solo        solo-template
     :production  production-template
     :query-group query-group-template}))

;https://docs.datomic.com/cloud/operation/new-system.html
;https://docs.datomic.com/cloud/releases.html#release-history
(defn create-storage [{:keys [storage-stack-name template]}]
  (aws/invoke
    cf
    {:op      :CreateStack
     :request {:Parameters   []
               :Capabilities ["CAPABILITY_NAMED_IAM"]
               :StackName    storage-stack-name
               :TemplateURL  template}}))

(defn create-compute [{:keys [compute-stack-name storage-stack-name keypair-name template]}]
  (aws/invoke
    cf
    {:op      :CreateStack
     :request {:Parameters   [{:ParameterKey "SystemName" :ParameterValue storage-stack-name}
                              {:ParameterKey "KeyName" :ParameterValue keypair-name}]
               :Capabilities ["CAPABILITY_NAMED_IAM"]
               :StackName    compute-stack-name
               :TemplateURL  template}}))

(defn monitor-stack-creation [stack]
  (loop [{:keys [StackStatus] :as status} (stack-status-1 stack)]
    (case StackStatus
      ("CREATE_IN_PROGRESS" "ROLLBACK_IN_PROGRESS")
      (do
        (println (format "Stack status is %s." StackStatus))
        (Thread/sleep 5000)
        (recur (stack-status-1 stack)))
      ("CREATE_COMPLETE" "ROLLBACK_COMPLETE")
      (do
        (println (format "Stack %s created successfully." StackStatus))
        status)
      (do
        (println (format "Unhandled completion status %s." StackStatus))
        status))))

(defn provision [{:keys [system-name keypair-name system-type] :or {system-type :solo}}]
  {:pre [(#{:solo :production} system-type)
         (some (fn [{:keys [KeyName]}] (#{keypair-name} KeyName)) (keypairs))]}
  (let [{:keys [storage] :as templates} (latest-datomic-cloudformation-templates)
        compute-template (templates system-type)
        storage-stack-result (create-storage {:storage-stack-name system-name
                                              :template           storage})
        {storage-stack-creation-status :StackStatus} (monitor-stack-creation storage-stack-result)
        compute-stack-result (create-compute {:compute-stack-name (str system-type "-compute")
                                              :storage-stack-name system-name
                                              :template           compute-template})]
    (monitor-stack-creation compute-stack-result)))