(ns itonami.legal-entity.gleif
  "GLEIF の LEI レコード（API https://api.gleif.org/api/v1/lei-records の data[] 1 件、
  JSON をキーワード化した map）を共通スキーマに正規化する。データは CC0 1.0。"
  (:require [clojure.string :as str]
            [itonami.legal-entity.ids :as ids]
            [itonami.legal-entity.schema :as schema]))

(def attribution
  "Contains LEI data from GLEIF (https://www.gleif.org/), CC0 1.0. Normalized by Awai Network.")

(def statuses {"ACTIVE" :active "INACTIVE" :inactive})

(defn- blank->nil [s] (when-not (str/blank? s) s))

(defn normalize
  "戻り値は {:record r} / {:reject reason}。"
  [{:keys [attributes]}]
  (let [{:keys [lei entity registration]} attributes
        juris (:jurisdiction entity)
        country (when (string? juris) (subs juris 0 (min 2 (count juris))))
        addr (:legalAddress entity)
        reg-no (blank->nil (:registeredAs entity))]
    (if-not (ids/lei? lei)
      {:reject (str "LEI が不正: " (pr-str lei))}
      (let [r (cond-> {:entity/id (str "lei:" lei)
                       :entity/jurisdiction country
                       :entity/name (get-in entity [:legalName :name])
                       :entity/identifiers (cond-> [{:scheme :lei :value lei}]
                                             reg-no (conj {:scheme :registration-number
                                                           :registry (get-in entity [:registeredAt :id])
                                                           :value reg-no}))
                       :entity/status (get statuses (:status entity) :unknown)
                       :source {:id :gleif
                                :updated-on (some-> (:lastUpdateDate registration) (subs 0 10))
                                :registration-status (:status registration)
                                :attribution attribution
                                :modified? true}}
                (not= juris country) (assoc :entity/jurisdiction-detail juris)
                (get-in entity [:legalForm :id]) (assoc :entity/legal-form {:scheme :iso-20275-elf
                                                                            :code (get-in entity [:legalForm :id])})
                addr (assoc :entity/address (cond-> {:country (:country addr)}
                                              (:region addr) (assoc :region (:region addr))
                                              (:city addr) (assoc :locality (:city addr))
                                              (seq (:addressLines addr)) (assoc :street (str/join " " (:addressLines addr)))
                                              (:postalCode addr) (assoc :postal-code (:postalCode addr))))
                (:creationDate entity) (assoc :entity/registered-on (subs (:creationDate entity) 0 10)))
            ps (schema/problems r)]
        (if (seq ps) {:reject (str/join "; " ps)} {:record r})))))
