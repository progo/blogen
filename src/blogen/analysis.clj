(ns blogen.analysis
  "Provide functionality for the second-phase analysis of post
  collection. Everything culminates in public function
  `analyze-posts'"
  (:use
   [blogen.config])
  (:require
   [blogen.sort :as sort]
   [blogen.post :as post]
   [blogen.versions :as versions]))

;; suggestions, todo:
;; - similar articles (by tag intersection and perhaps title exploding)

(defn analyze-posts
  "Given input seq of posts, collect and generalize data within the
  context of all posts."
  [posts]
  (let [creation-order (vec (sort sort/by-creation-date posts))]
    (for [post posts]
      (let [post-index (.indexOf creation-order post)]
        (-> post
            (assoc-in [:order-created :next]
                      (-> (get creation-order (dec post-index)) :path))
            (assoc-in [:order-created :prev]
                      (-> (get creation-order (inc post-index)) :path)))))))