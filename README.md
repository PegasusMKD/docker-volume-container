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

## Set Up
 I use Linux, so the project might be harder to set up on Windows (Docker is weird for setting up on Windows). But, all you might need is to:
 - Import from the pom.xml file using maven
 - Create a package of the server code - ```mvn package```
 - Unpack the package (not sure about how the command would be on a Windows system, used it from the [Spring docs](https://spring.io/guides/gs/spring-boot-docker/)) - ```mkdir -p target/dependency && (cd target/dependency; jar -xf ../*.jar)```
 - And finally build the image - for me: ```docker build -t pazzio/docker-test .``` (if you change the name of the image, change it in the back-end code aswell)
 
 And then just start the server (all of the dependencies should be good).
 Some of the dependencies might not be needed, but ones that are a must (and don't come with a spring boot project) are:
  - com.google.guava.guava
  - [com.spotify.docker-client](https://github.com/spotify/docker-client)
 

## User Manual

#### Path: GET /clear
 Just for cleaning up all of the containers (so i can more easily manage what containers to start/stop etc. instead of having 20 containers just sitting there)

#### Path: GET /clear-vol
 Just for cleaning up all of the volumes. You first have to stop running containers, then call **GET /clear** and then finally call this path.

#### Path: GET /
 This path creates a container volume using the ['busybox'](https://hub.docker.com/_/busybox) image, and sets a volume to the container under the path of '/file-system'. It also creates the service, and sets the ['busybox'](https://hub.docker.com/_/busybox) container as a volume for it. **The ['busybox'](https://hub.docker.com/_/busybox) container won't be able to start up, I don't know why it's like this, but it is, i wasted about an hour wondering about this. It seems like when you set it up as a volume for another container, it keeps killing the main process on it which keeps it alive**.

 If this path returned 'true', that means that the containers were created without any warnings. The next step is to get the IP of the service container.
 Typing ``` docker ps ``` in the Terminal should give you a list of all the active containers, and you should search for the one with an entry point simmilar to the one in the Dockerfile. When you find it, copy the **CONTAINER ID** and then type ``` docker inspect <id> ``` and then search the JSON for a IPv4 address. Save   it in some notepad or something.

#### Path: GET ip address of the service/create-file
 This path creates the script and some directories in the volume. If it returns 'true', that means that it was created.
 
#### Path: GET ip address of the service/run-script
 This path should return the structure of the volume (which would be treated as a file system) as a HashMap('parentFolder':'child files and child folders').
 
## Personal notes about the code

 I use the same code as both the **Manager** and the **Container** service since this is just a POC, and doing them seperately would just be "too much work".
 I have a shell script for building the docker image called build-docker.sh.
 Two big notes that definetely won't be the way as they are now, I did them like this because I wasn't sure as to how to fix them ,or I was too lazy to search for a solution:
  - the Back-end Service image uses root
    - I did this because Docker made the volume accessible only by root, but this might get fixed if we made a custom image of [busybox](https://hub.docker.com/_/busybox), and had it start-up using a different user in a mutual group between the service and volume
  - Creating the scripts using the service
    - I did this just for testing reasons (whether i can create files and folders in the folder)
   
