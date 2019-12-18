(ns ls
  (:require
    [clojure.java.shell :as sh]))

(defn -main [& args]
  (let [{:keys [out err exit]} (apply sh/sh "ls" args)]
    (println out)
    (System/exit exit)))