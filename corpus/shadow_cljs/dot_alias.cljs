(ns shadow-cljs.dot-alias
  (:require ["dayjs" :as dayjs]
            ["dayjs/plugin/timezone" :as dayjs.timezone]
            ["dayjs/plugin/utc" :as dayjs.utc]))

(.extend dayjs dayjs.utc)
(.extend dayjs dayjs.timezone)

(require '["dayjs/plugin/utc2" :as dayjs.utc2])
(.extend dayjs dayjs.utc2)
