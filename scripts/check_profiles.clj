(ns check-profiles
  "hermes/profiles/* の定義を検査する。
  - itonami.edn の :capabilities と yakuwari.edn の :yakuwari/capabilities が同じ
  - 既定は :blocked、cron は無効、推論は murakumo のみ
  - 公開・アップロード・支払い・送信・署名・削除・cron・git 書き込みは :blocked"
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def must-block
  #{:publish :storage_upload :spend :transfer_funds :external_messaging
    :use_signing_identity :delegate_task :manage_services
    :export_private_keys :delete_data :enable_cron :git_push :git_merge})

(defn- problems [dir]
  (let [id (.getName dir)
        rd #(edn/read-string (slurp (io/file dir %)))
        it (rd "itonami.edn")
        yk (rd "yakuwari.edn")
        caps (:capabilities it)
        decision (into {} (map (juxt :capability :decision)) caps)
        jobs (slurp (io/file dir "cron/jobs.json"))
        config (slurp (io/file dir "config.yaml"))]
    (cond-> []
      (not= id (:profile/id it)) (conj "profile/id がディレクトリ名と違う")
      (not= caps (:yakuwari/capabilities yk)) (conj "itonami.edn と yakuwari.edn の capability が違う")
      (not= :blocked (get-in it [:authority :default-decision])) (conj "既定の判断が :blocked でない")
      (:cron? it) (conj ":cron? が true")
      (not= "{\"jobs\":[]}" (str/trim jobs)) (conj "cron/jobs.json が空でない")
      (not= [:murakumo] (get-in it [:inference :rails])) (conj "推論 rail が murakumo だけでない")
      (str/includes? config "openrouter") (conj "config.yaml に openrouter がある")
      (seq (remove #(= :blocked (decision %)) must-block))
      (conj (str "blocked でない: " (vec (remove #(= :blocked (decision %)) must-block)))))))

(defn -main [& _]
  (let [dirs (filter #(.isDirectory %) (.listFiles (io/file "hermes/profiles")))
        found (for [d (sort-by #(.getName %) dirs)] [(.getName d) (problems d)])
        bad (filter (comp seq second) found)]
    (doseq [[id ps] found]
      (println (if (seq ps) "NG" "OK") id (str/join "; " ps)))
    (System/exit (if (seq bad) 1 0))))
