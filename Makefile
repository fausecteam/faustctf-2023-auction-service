SERVICE := auction-service
DESTDIR ?= dist_root
SERVICEDIR ?= /srv/$(SERVICE)

.PHONY: build install

build:
	echo nothing to build

install: build
	mkdir -p $(DESTDIR)$(SERVICEDIR)
	bash -c "cp service/{docker-compose.yml,Dockerfile,client.sh,server.sh} $(DESTDIR)$(SERVICEDIR)"
	mkdir -p $(DESTDIR)$(SERVICEDIR)/src/
	cp -r service/src/de/ $(DESTDIR)$(SERVICEDIR)/src/
	cp README.md $(DESTDIR)$(SERVICEDIR)
	mkdir -p $(DESTDIR)/etc/systemd/system/faustctf.target.wants/
	ln -s /etc/systemd/system/docker-compose@.service $(DESTDIR)/etc/systemd/system/faustctf.target.wants/docker-compose@auction-service.service

