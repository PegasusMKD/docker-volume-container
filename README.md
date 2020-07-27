# docker-volume-container

## Concept

The concept is comprised of 4 parts:
- Spring Boot Server (serving as a manager)
- Docker Volume Container
- Shell scripts
- Back-end server (serving as the actual server)

With this type of system i think it will be easy to manage, redundant and safe. We could even extend upon it to allow them to use it as a full-blown storage service.

#### Spring Boot Server (manager)

The idea is that the (as I like to call it) "manager" server will do crane jobs (for example creating .tars of the containers,
pulling up new images/containers if needed, building them, etc.) which will help for having redundancy, so even in the event of a failure in the system
the manager can just turn up the last snapshot of whichever container went down. (I'm sure that we could use it for even more functionality than that)

#### Docker Volume Container

Docker has a feature called 'volumes' for persisting data (since when a container "dies" it loses everything stored within it, and it rebuilds). This is usually used for
keeping database. How it works, on a concept level, either you designate a two paths for it, or it designates the host one on it's own, and you just designate the one on the
container. And it mounts those two paths. The container sees the volume's pathing as just a regular folder, with which it can do any type of IO.

But, since this kindof ruins the concept of isolation which docker provides, they give the option to implement them in a bit of different way.

You can use parts of the container as volumes, thus getting isolation, and beside the isolation, you also get all of the features of a container (easy back-ups for example),
and you can share volumes, so, if we want to add a volume to that container for keeping the images for the angular front-end, we can use the same container.

I think that the 'busybox' image is great, it's lightweight (1.22 Mb) and gives us most of the unix commands which we might need.

#### Shell Scripts

I think that this is the best possible solution. In the Google Docs I mentioned that i tried multiple options (using a VM, using the JVM, using Docker), and none of them
provide a functionality for traversing through the file system/volume, getting a file, etc.

So we could maybe keep all of the shell functionality scripts in a folder inside of the resource folder, or maybe we can copy them inside of the container volume when building it.

And then, dependending on the needs, call them using the Java Runtime, and get the results from it through the input stream of the runtime.

#### Back-end Server

This is the service with the main bussiness logic. It will contain all of the shell scripts and it can make use of them as it needs to.

I mention a possible way to do this in the comments of the service. Basically keep the possible commands in a HashMap, and then dependent on the url parameter given, run the appropriate script and return the result. (It should always be a String)

## Docker Container Volumes vs.

#### VMs (more specifically i tested out Virtual Box)

 - Easier for setting up (no need to set-up a whole VM, and even then, since we would be using Docker, it's data wouldn't be persisted)
 - Much more control (easier for back-ups)
 - Spotify's SDK is MUCH better and a lot more complete compared to the VirtualBox SDK (in the VB SDK some functionalities are implemented in the middleware, but not in the back-end, some are the other way around, different types of connectivity (HTTP and COM ports))
 
#### Saving them on the host machine

 - I have safety worries with this method since we can't know what files they might upload (even if we restrict the type)
 - No isolation
 - Harder to back-up (in my opinion)
 - With the Host approach, we'd have to keep the system's specific file pathing/structure
 
#### JVMs

- Didn't explore this option too much, but it just sounded wrong (will add more info if we want to look into this)


## Personal notes about the code

 
 
