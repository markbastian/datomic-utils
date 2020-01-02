(ns startup-system
  (:require [datomic.utils.ec2 :as ec2]
            [datomic.utils.autoscaling :as autoscaling]
            [clojure.string :as cs]
            [clojure.tools.cli :refer [parse-opts]]))

(def cli-options
  [["-s" "--system SYSTEM" "Datomic system name"]
   ["-h" "--help"]])

(defn -main [& args]
  (let [{{:keys [system help]} :options} (parse-opts args cli-options)
        asgs (seq (autoscaling/describe-autoscaling-groups system))
        systems (set (ec2/instances))]
    (cond
      asgs
      (do
        (println (format "Starting system named %s." system))
        (autoscaling/startup-datomic-system system))
      help
      (println "clj -m startup-system -s $system-name")
      :else
      (println (format
                 "No Datomic system named %s. Available systems are: %s."
                 system
                 (cs/join ", " (sort systems)))))
    (System/exit 0)))