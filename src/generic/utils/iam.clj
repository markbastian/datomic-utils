(ns generic.utils.iam
  (:require [cognitect.aws.client.api :as aws]))

(defonce iam (aws/client {:api :iam}))

(defn users []
  (let [{:keys [Users]} (aws/invoke iam {:op :ListUsers})]
    (map :UserName Users)))