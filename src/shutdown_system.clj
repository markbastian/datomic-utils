(ns shutdown-system
  (:require [datomic.utils.ec2 :as ec2]
            [datomic.utils.autoscaling :as autoscaling]
            [clojure.string :as cs]
            [clojure.tools.cli :refer [parse-opts]]))

(def cli-options
  ;; An option with a required argument
  [["-s" "--system SYSTEM" "Datomic system name"]
   ["-h" "--help"]])

(defn -main [& args]
  (let [{{:keys [system help]} :options} (parse-opts args cli-options)
        systems (set (ec2/instances))]
    (println help)
    (cond
      (systems system)
      (do
        (println (format "Shutting down system named %s." system))
        (autoscaling/shutdown-datomic-system system))
      help
      (println "clj -m shutdown-system -s $system-name")
      :else
      (println (format
                 "No running Datomic system named %s. Available systems are: %s."
                 system
                 (cs/join ", " (sort systems)))))
    (System/exit 0)))