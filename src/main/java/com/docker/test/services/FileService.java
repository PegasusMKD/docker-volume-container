package com.docker.test.services;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.*;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class FileService {
    final DockerClient docker = DefaultDockerClient.builder().uri("unix:///var/run/docker.sock").build();

    /**
     * Just an experimental function for testing out the capabilities of the possible SDKs (currently on the Spotify SDK)
     *
     * @throws DockerException      - from Spotify SDK
     * @throws InterruptedException - from Spotify SDK
     */
    @Deprecated
    public String startContainer() throws DockerException, InterruptedException {
        final List<Container> containers = docker.listContainers(DockerClient.ListContainersParam.allContainers());
        final List<Image> quxImages = docker.listImages();
        final Image img = quxImages.stream().filter(image -> Iterables.contains(Objects.requireNonNull(image.repoTags()), "pazzio/docker-test:latest")).collect(Collectors.toList()).get(0);
        ContainerCreation container = null;
        Container existingContainer = null;
        if (containers.isEmpty()) {
            container = docker.createContainer(ContainerConfig.builder().image(img.id()).build());
            if (Objects.requireNonNull(container.warnings()).isEmpty()) {
                docker.startContainer(container.id());
                System.out.println("Started a container!");
            }
            System.out.println("Created a container!");
        } else {
            existingContainer = containers.stream().filter(container1 -> Objects.equals(container1.imageId(), img.id())).collect(Collectors.toList()).get(0);
            if (!Objects.equals(existingContainer.state(), "running")) {
                docker.startContainer(existingContainer.id());
                System.out.println("Started a container!");
                // Basically the plan is:
                // Create one with a ContainerConfig and a specific volume (or maybe just a path, and docker will start using that path as containers get added)
                // Create all of the others with a HostConfig and a volumesFrom ( and we might need a .binds() )
                HostConfig.builder().volumesFrom().build(); // This is what we'll need
            } else {
                System.out.println("Already started!");
            }
        }

        return container == null ? existingContainer.id() : container.id();
    }

    /**
     * Function for creating a container volume, as well as the back-end container, and link them
     *
     * <p>
     * Just makes my life easier not having to do all of this using the command line
     * </p>
     *
     * <p>
     * It might be smarter to make a Dockerfile for the container volume in which
     * we'd define a user group, so that the back-end container doesn't have to be
     * as the root user (to have permission to write and read files from the volume).
     * </p>
     *
     * <p>
     * if it was successful, go into the terminal and type:
     * 'docker ps' -> get the container ID
     * 'docker inspect <id>' -> and search for the IPv4 address of the container so you can start using it
     * </p>
     *
     * <p>
     * I'm using the same back-end for the host and container since I'm too lazy to setup 2 special back-ends for
     * a POC with the file system
     * </p>
     *
     * @return whether it successfully created the containers
     * @throws DockerException      - from Spotify SDK
     * @throws InterruptedException - from Spotify SDK
     */
    public boolean startVolumeContainer() throws DockerException, InterruptedException {
        // Create the container volume
        ContainerCreation containerVolume = docker.createContainer(ContainerConfig.builder()
                .image("busybox").volumes("/file-system").build());
        if (!Objects.requireNonNull(containerVolume.warnings()).isEmpty()) {
            return false;
        }

        // Find the back-end image
        final Image img = docker.listImages().stream().filter(image -> Iterables.contains(Objects.requireNonNull(image.repoTags()),
                "pazzio/docker-test:latest")).collect(Collectors.toList()).get(0);

        // Configure port forwards for the back-end container and volume mountings
        Map<String, List<PortBinding>> portBindings = Maps.newHashMap();
        portBindings.put("8080/tcp", Lists.newArrayList(PortBinding.of("", "80")));
        HostConfig hostConfig = HostConfig.builder()
                .portBindings(portBindings)
                .networkMode("bridge")
                .volumesFrom(containerVolume.id())
                .build();

        // Create the back-end container, and set it up so it uses the volume from the container volume
        ContainerCreation backEndContainer = docker.createContainer(ContainerConfig.builder()
                .image(img.id()).hostConfig(hostConfig).exposedPorts("8080").build());
        if (!Objects.requireNonNull(backEndContainer.warnings()).isEmpty()) {
            return false;
        }

        docker.startContainer(backEndContainer.id());

        return true;
    }

    /**
     * Function for deleting all the generated (active and inactive) volumes
     * <p>
     * Mostly used for cleaning up, instead of doing a
     * 'docker volume ls' -> 'docker volume rm <id>'
     * for each volume
     *
     * @throws DockerException      - from Spotify SDK
     * @throws InterruptedException - from Spotify SDK
     */
    public void cleanVolumes() throws DockerException, InterruptedException {
        cleanContainers();
        final VolumeList containers = docker.listVolumes();
        for (Volume container1 : Objects.requireNonNull(containers.volumes())) {
            docker.removeVolume(container1);
        }

    }

    /**
     * Function for deleting all the generated (active and inactive) containers
     * <p>
     * Mostly used for cleaning up, instead of doing a
     * 'docker ps -a' -> 'docker kill <id>' || 'docker rm <id>'
     * for each container
     *
     * @throws DockerException      - from Spotify SDK
     * @throws InterruptedException - from Spotify SDK
     */
    public void cleanContainers() throws DockerException, InterruptedException {
        final List<Container> containers = docker.listContainers(DockerClient.ListContainersParam.allContainers());
        for (Container container1 : containers) {
            docker.removeContainer(container1.id());
        }
    }

    /**
     * This creates a script and folders which are meant to be used for testing
     * <p>
     * The point is to check whether i can create directories and files in the mounted volume, whether it recognizes
     * the volume, and then use one of the created files to test out whether the functionality works properly
     * </p>
     *
     * @return whether it successfully created the script and folder
     * @throws IOException - if it can't find the file (i think)
     */
    public boolean testFileFolder() throws IOException {
        // Create the script
        String shellScript = "ls -R /file-system";
        Path path = Paths.get("/file-system/directories.sh");
        boolean result = new File("/file-system/directories.sh").createNewFile();
        if (!result) {
            return false;
        }
        Files.write(path, shellScript.getBytes());

        // Create some directories for testing
        for (int i = 0; i < 10; i++) {
            Path path1 = Paths.get(String.format("/file-system/%d", i));
            Files.createDirectory(path1);
        }

        return true;
    }

    /**
     * Function that calls a shell script, and returns its result
     * <p>
     * The idea here is that we could have shell or bash scripts (most probably in the resource folder or directly in a custom volume container image)
     * with which we get to interact with the underlying file system (which is the volume) since everything I've searched through doesn't give that kind of
     * functionality natively.
     * </p>
     *
     * <p>
     * Some examples:
     *     <ul>
     *         <li>Wanting to get the system's (file system that will be available as display) hierarchy</li>
     *         <li>Moving files (also available and easy to do through Java)</li>
     *         <li>Making directories with special permissions even on the underlying server</li>
     *         <li>Defining special environment variables at specific times</li>
     *     </ul>
     * </p>
     *
     * <p>
     *     Maybe we could keep the possible commands in a HashMap, something like this
     *     {'tree': {'sh', './resources/tree.sh'},
     *     'move': {'sh', './resources/move.sh'},...}
     *     and the middleware can just do a GET request, with the key as a url parameter, or maybe
     *     have it be a POST request, and have the key be sent as a pair, ex. {'key': 'tree'}
     *     with which a lot of safety problems get covered, as well as complexity on the middleware side
     * </p>
     *
     * @return a mapping of the tree of the file-system
     * @throws IOException          - from Spotify SDK
     * @throws InterruptedException - from Spotify SDK
     */
    public HashMap<String, List<String>> runScript() throws IOException, InterruptedException {
        // Define command
        String[] script = {"sh", "/file-system/directories.sh"};

        // Run the command using the Java Runtime
        Process cmd = Runtime.getRuntime().exec(script);
        cmd.waitFor();

        // Get the cmd line stdin
        BufferedReader reader = new BufferedReader(new InputStreamReader(cmd.getInputStream()));

        // Start building the map
        HashMap<String, List<String>> data = new HashMap<>();
        String line;
        List<String> folderBuffer = new ArrayList<>();
        boolean topFolder = true;
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty()) {
                continue;
            }
            if (line.contains(":")) {
                if (topFolder) {
                    topFolder = false;
                    folderBuffer.add(line);
                    continue;
                }
                String folderName = folderBuffer.get(0);
                folderBuffer.remove(0);
                data.put(folderName, folderBuffer);
                folderBuffer = new ArrayList<>();
            }
            folderBuffer.add(line);
        }

        return data;
    }
}
