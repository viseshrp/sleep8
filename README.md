# sleep8
When the user **arms** the app (button or Quick Settings tile), the app watches for **screen-off** events during a **fixed night window**; once the screen has stayed off for **10 minutes**, it schedules a **real OS alarm** for **8 hours after the original screen-off time**, and re-schedules from the **latest** screen-off event.
