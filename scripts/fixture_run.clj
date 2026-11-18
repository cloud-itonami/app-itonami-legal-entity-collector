(ns fixture-run
  "架空データで collector を通した結果を EDN で書く。CI は docs/evidence と差分を取る。"
  (:require [clojure.edn :as edn]
            [clojure.pprint :as pp]
            [itonami.legal-entity.collect :as collect]
            [itonami.legal-entity.fixtures :as fx]))

(defn -main [& _]
  (let [catalog (edn/read-string (slurp "sources/catalog.edn"))
        summarize (fn [{:keys [source records skipped rejected error]}]
                    (if error
                      {:source source :error error}
                      {:source source
                       :record-ids (mapv :entity/id records)
                       :skipped skipped
                       :rejected rejected}))]
    (pp/pprint
     {:fixture "fictional data only (test/itonami/legal_entity/fixtures.clj)"
      :runs [(summarize (collect/collect catalog :jp-nta (map fx/nta-lines (sort (keys fx/nta-lines)))))
             (summarize (collect/collect catalog :gleif (map fx/gleif-records (sort (keys fx/gleif-records)))))
             (summarize (collect/collect catalog :dk-cvr []))
             (summarize (collect/collect catalog :dnb []))]})))
