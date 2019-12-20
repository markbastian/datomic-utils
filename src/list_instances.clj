(ns list-instances
  (:require [datomic.utils.ec2 :as ec2]))



;https://github.com/cognitect-labs/aws-api/blob/master/latest-releases.edn
;Potential for replion
;:UpdateSecurityGroupRuleDescriptionsEgress
;:UpdateSecurityGroupRuleDescriptionsIngress

(defn -main [& args]
  (do
    (doseq [instance (ec2/instances)]
      (println instance))
    (System/exit 0)))