# 718 Custom Content
## Disclaimer
These projects are 99% authentic, with contributions from other
developers with a passion for recreating/creating content. They are
intended for **personal use and reference**.

The projects are distributed **AS-IS** and **FREE OF CHARGE**, and may
only be redistributed outside of this repository with my consent, and under
those same conditions.

Feel free to create your own branches if you wish to upload your own content.

### Pyramid Plunder
An instanced version of the Pyramid Plunder minigame. Custom rewards table,
slightly altered game mechanics.

### Araxxor
A recreation of the Araxxor boss with Legacy style combat. All three paths
included, although the mechanics are about 15% recreated, and slightly
modified to be doable on 718. **NOTE:** *There is a lot of custom content
used in this project which isn't my own, such as improved projectiles
and plugin scripts, as well as a custom threading system which I created.
I might upload the threading system, but for now you'll have to figure out how
to make this portable. For reference, any calls to 
`CoresManager.getServiceProvider().execute(() -> ...)` that you see are simply wrappers
around your game core's `ScheduledExecutorService`. Likewise with `ServiceProvider::executeWithDelay`.
`FixedLengthRunnable` is my own personal version of a `TimerTask`, and `return false` in the
body of the `repeat` method is comparable
to `TimerTask::cancel`. I made `FixedLengthRunnable` because I no longer use `Timer` as it will
be deprecated in JDK 9 and is incredibly inferior to `ExecutorService`*
