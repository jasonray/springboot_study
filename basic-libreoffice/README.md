### Info
this directory contains a replica of alpine based
[alpine based libreoffice container](https://github.com/woahbase/alpine-libreoffice). 
This container can be used to run `libreoffice` headlessly in batch mode. In the long run,
the original project `Dockerfile` needs to be modified with regards of `ENTRYPOINT` and `DISPLAY`
in order to enable to run headlessly from the Springboot app

### History

[Libreoffice](https://documentation.libreoffice.org/en/english-documentation/) recognizes `--headless` option, which stops its processes from expwcting the X windows
hosting the graphical user interface. It is often used in combination with other switches  when one can convert files between
several formats, including PDF, HTML, DOC, DOCX, EPUB, plain text, etc.

### Usage
* pull
```sh
docker pull woahbase/alpine-libreoffice:x86_64
```
this will bring a 700 Mb image
```text
woahbase/alpine-libreoffice            x86_64                 1136a70e3a54   21 months ago   708MB
```

* execute in batch mode
   + the command 
```sh
docker run --rm -it --name docker_libreoffice --hostname libreoffice -e PGID=1000 -e PUID=1000 -c 512 -m 2096m --headless -v /usr/share/fonts:/usr/share/fonts:ro -v data:/home/alpine\ --convert-to pdf /home/alpine/test.doc woahbase/alpine-libreoffice:x86_64
```

would fail because it has the LibreOffice arguments in the wrong placed

  + the command
```sh
docker run --rm -it --name docker_libreoffice --hostname libreoffice -e PGID=1000 -e PUID=1000 -c 512 -m 2096m -v /usr/share/fonts:/usr/share/fonts:ro -v data:/home/alpine woahbase/alpine-libreoffice:x86_64 /usr/bin/libreoffice  --headless  --convert-to pdf /home/alpine/test.doc
```
will fail complaining
```text
Error: source file could not be loaded
```
  
  + the command
```sh
docker run --rm --entrypoint  sh  -it --name docker_libreoffice --hostname libreoffice -e PGID=1000 -e PUID=1000 -c 512 -m 2096m -v /usr/share/fonts:/usr/share/fonts:ro -v data:/home/alpine  woahbase/alpine-libreoffice:x86_64

```

continued inside the container
```sh
/usr/bin/libreoffice  --headless  --convert-to pdf /home/alpine/test.doc
```

succeeds

```text
convert /home/alpine/test.doc -> /home/alpine/test.pdf using filter : writer_pdf_Export
```
   + the fixed command

```sh
docker run --rm -it --name docker_libreoffice --hostname libreoffice -e PGID=1000 -e PUID=1000 -c 512 -m 2096m -v /usr/share/fonts:/usr/share/fonts:ro -v $(pwd)/data:/home/alpine woahbase/alpine-libreoffice:x86_64 /usr/bin/libreoffice  --headless  --convert-to pdf /home/alpine/test.doc

```
works but does not quit libreoffice proces after conversion completion  - one can copy the file and terminate container


  + the fixed command
```sh
docker run --entrypoint  sh --rm -it --name docker_libreoffice --hostname libreoffice -e PGID=1000 -e PUID=1000 -c 512 -m 2096m -v /usr/share/fonts:/usr/share/fonts:ro -v $(pwd)/data:/home/alpine woahbase/alpine-libreoffice:x86_64 /usr/bin/libreoffice  --headless  --convert-to pdf /home/alpine/test.doc
```
works and does not have this issue

### Building Container

use the stock alpine JRE image, temporarily skip certain fonts by commenting the line
```sh
RUMN apk add --no-cache -U ttf-font-awesome ttf-mononoki ttf-hack
```

```sh
IMAGE=basic-libreoffice
docker build -f Dockerfile -t $IMAGE .
```
```sh
docker run --entrypoint  sh --rm -it --name docker_libreoffice --hostname libreoffice -e PGID=1000 -e PUID=1000 -c 512 -m 2096m -v /usr/share/fonts:/usr/share/fonts:ro -v $(pwd)/data:/home/alpine $IMAGE /usr/bin/libreoffice  --headless  --convert-to pdf /home/alpine/test.doc
```
currently prints few errors but completes the task:
```text
WARNING: Your kernel does not support swap limit capabilities or the cgroup is not mounted. Memory limited without swap.
javaldx: Could not find a Java Runtime Environment!
Warning: failed to read path from javaldx
convert /home/alpine/test.doc -> /home/alpine/test.pdf using filter : writer_pdf_Export
```
![Docker Cluster](https://github.com/sergueik/springboot_study/blob/master/basic-libreoffice/screenshots/capture-libreoffice-container.png)



### Web UI LibreOffice Container

The Libreoffice container with Web UI is already alpine based

`lscr.io/linuxserver/libreoffice` ?
https://hub.docker.com/r/linuxserver/libreoffice/tags

```sh
docker pull lscr.io/linuxserver/libreoffice
```
```sh
docker run -d -p  3000:3000 lscr.io/linuxserver/libreoffice:latest
```

the `http://localhost:3000/` will show the desktop


![Libreoffice Web UI](https://github.com/sergueik/springboot_study/blob/master/basic-libreoffice/screenshots/capture-libreoffice-webui.png)

### Posting Upload via curl

* the correct way
```sh
curl -s -XPOST -F 'operation=send' -F 'param=data' -F "file=@$(pwd)/data.txt" http://localhost:8085/basic/upload
```
* NOTE: cannot combine request parameters in `-F`:
```sh
curl -s -XPOST -F "operation=send&param=data&file=@$(pwd)/data.txt" http://localhost:8085/basic/upload
```
```JSON
{
  "timestamp": "2023-01-19T00:36:10.177+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "",
  "path": "/basic/upload"
}
```
with application console log:
```text
ParameterException: Required String parameter 'param' is not present]
```
* NOTE: cannot issue both `-d` and  `-F`:


```sh
curl -s -X POST -F "file=$(pwd)/data.txt" http://localhost:8085/basic/upload -d "operation=send"
```
```text
Warning: You can only select one HTTP request method! You asked for both POST
Warning: (-d, --data) and multipart formpost (-F, --form).
```
with request not being sent

* download binary file, instruct the server to encode it when logging
```sh
base64 1x1.png
```
```text
iVBORw0KGgoAAAANSUhEUgAAAAEAAAABAQMAAAAl21bKAAAAA1BMVEUAAACnej3aAAAAAXRSTlMA
QObYZgAAAApJREFUCNdjYAAAAAIAAeIhvDMAAAAASUVORK5CYII=
```
invoke as
```sh
 curl -XPOST -F "operation=send" -F "param=data" -F "encode=true" -F "file=@$(pwd)/1x1.png" http://localhost:8085/basic/upload
```
on the server end see
```text
herServlet        : Complet
Processing 1x1.png
size:95/95
raw data(base64 encoded):
[B@45e171a6
```
 - not quite right, but close enough
### See Also

  * https://stackoverflow.com/questions/50982064/converting-docx-to-pdf-with-pure-python-on-linux-without-libreoffice
  * https://stackoverflow.com/questions/72434000/running-libreoffice-converter-on-docker  
  * https://docs.linuxserver.io/images/docker-libreoffice
  * https://medium.com/codex/libreoffice-on-docker-1a64245468c
  * [known bug](https://bugs.documentfoundation.org/show_bug.cgi?id=37531&redirected_from=fdo) of Libreoffice will not run in "batch mode" when there is another instance open
  * [document](https://www.baeldung.com/spring-file-upload) and [source](https://github.com/eugenp/tutorials/tree/master/spring-web-modules/spring-mvc-java) on Spring App file Upoad - too many projects in the same umbrella
  * [upload files through curl](https://medium.com/@petehouston/upload-files-with-curl-93064dcccc76)
  * [spring uloading gettin files started](https://spring.io/guides/gs/uploading-files/)
  * [1 pixel png used as spacer](https://commons.wikimedia.org/wiki/File:1x1.png)
### Author
[Serguei Kouzmine](kouzmine_serguei@yahoo.com)
