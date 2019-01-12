Hello, Traveller.

So this repository branch is for development of *new user interface* for artemis-odb debugger. It was 
triggered by new powers by new *serializer* I've developed that was pretty much universal (you can read 
about that on my blog). I've imagined many features for 
this on top of artemis-odb but the user interface was something that bugged me. So I'm changing it.


# The story of huge refactor

Originally, the interface was made with Java Swing. Swing was buggy, took lots of time on every small 
change in layout design and internals of Java Swing seemed to create lots of memory garbage. I was 
somewhat OK with performance since I neved tried a huge/real project (yet). But I totally didn't felt OK 
with the speed of development. I wished for declarative UI. So I did few things:

- moved from Java to Kotlin
- refactored everything to kotlin-common which enabled me to build both JVM server and JavaScript client
- ported snabbdom (Virtual DOM library) and elm-ui to Kotlin and coupled them together

This was tough decision since it's no more possible to run UI within game process. On the other hand, It 
felt very convenient when I had to only restart the game during development and debugger was there waiting 
in the background. So now everything is going over network which was just an option before.


## What's next?

At the moment of writing this I have almost redone old UI to but I'm *thinking forward*. I'd be happy to 
hear suggestions for next features. What are *your usecases of debugging*?

This is what I'm planning on:

- modifying component's values
- adding/removing entity components
- marking entities I want to track, getting noticed when they get destroyed
- filtering entity list
- searching for entities having certain values
- remembering some things for diffing their states later
- pausing the world
- turning on/off managers and systems
- calling in-game procedures written by user and registered to the debugger
