# Background work

`ReminderWorker` (WorkManager, roughly hourly) posts the daily mission and streak reminders
(F-COM-12). The decision rules live in `domain/session/ReminderPolicy` and are unit-tested; the
worker only reads state, remembers what was sent today and shows the notification. Music playback
belongs to `MusicPlaybackService` in `core:media`, not to a worker. Server synchronisation is not
implemented: there is no real backend yet.
