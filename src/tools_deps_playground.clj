(ns tools-deps-playground
  (:require
    [clojure.java.shell :as sh]))

(defn -main
  [& args]
  (let [{:keys [out err exit]} (sh/sh "ls" "-al")]
    (println out)
    (System/exit exit)))