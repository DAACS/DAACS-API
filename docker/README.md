`bash build.sh`
`docker run --name daacsapi -v /Volumes/shared/Configs/docker:/config -d -p 8080:8080 -e daacsapiProperties=/config/daacs.properties daacsapi`
