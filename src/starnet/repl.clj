(ns starnet.repl
  (:require
   [clojure.pprint :as pp]
   [clojure.spec.alpha :as s]
   [clojure.core.async :as a :refer [<! >! <!! timeout chan alt! go
                                     >!! <!! alt!! alts! alts!! take! put!
                                     thread pub sub]])
  )

(comment

  create-user
  delete-account
  change-username
  change-email
  list-users
  list-user-account
  list-user-ongoing-games
  list-user-game-history
  create-event
  :event.type/single-elemination-bracket
  :event/start-ts
  cancel-event
  signin-event
  signout-event
  list-events
  list-event-signedup-users
  create-game
  cancel-game
  start-game
  end-game
  list-games
  join-game
  invite-into-game
  connect-to-game
  disconnect-from-game
  ingame-event
  list-ingame-events-for-game
  
  ;;
  )

(def games {0 #uuid "15108e92-959d-4089-98fe-b92bb7c571db"
            1 #uuid "461b65a8-0f24-46c9-8248-4bf6d7e1aa1a"})

(def users {0 #uuid "5ada3765-0393-4d48-bad9-fac992d00e62"
              1 #uuid "179c265a-7f72-4225-a785-2d048d575854"})

(def observers {0 #uuid "46855899-838a-45fd-98b4-c76c08954645"
                1 #uuid "ea1162e3-fe45-4652-9fa9-4f8dc6c78f71"
                2 #uuid "4cd4b905-6859-4c22-bae7-ad5ec51dc3f8"})