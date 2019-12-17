(ns list-instances
  (:require
    [clojure.java.shell :as sh]))

(defn -main
  [& args]
  (let [{:keys [out err exit]}
        (sh/sh
          "aws"
          "ec2" "describe-instances"
          "--filters" "Name=tag-key,Values=datomic:tx-group"
          "Name=instance-state-name,Values=running"
          "--query" "Reservations[*].Instances[*].[Tags[?Key==`datomic:system`].Value]"
          "--output" "text")]
    (println out)
    (System/exit exit)))