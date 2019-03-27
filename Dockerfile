FROM ubuntu:latest
RUN apt-get update && apt-get full-upgrade -y
RUN yes | apt-get install openjdk-8-jdk
RUN yes | apt-get install wget
RUN yes | apt-get install libxss1 libgconf2-4 libappindicator1 libindicator7 fonts-liberation libappindicator3-1 lsb-release xdg-utils
RUN yes | apt-get install xvfb xfonts-100dpi xfonts-75dpi xfonts-scalable xfonts-cyrillic
RUN wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb
RUN yes | dpkg -i --force-depends google-chrome-stable_current_amd64.deb
RUN yes | apt-get install language-pack-ru
RUN yes | apt-get install tzdata
RUN yes | ln -fs /usr/share/zoneinfo/Europe/Moscow /etc/localtime && dpkg-reconfigure -f noninteractive tzdata
RUN sed -i -e 's/# ru_RU.UTF-8 UTF-8/ru_RU.UTF-8 UTF-8/' /etc/locale.gen && locale-gen
RUN apt-get clean
ADD chromedriver /usr/local/bin/
RUN chmod 755 /usr/local/bin/chromedriver
RUN mkdir -p /srv/parsers
ADD parser_all-1.0-jar-with-dependencies.jar /srv/parsers/
ADD settings.json /srv/parsers/
RUN touch /etc/default/locale
RUN echo LANG="ru_RU.UTF-8" >> /etc/default/locale
RUN echo LC_ALL="ru_RU.UTF-8" >> /etc/default/locale
ENV LANG ru_RU.UTF-8
ENV LANGUAGE ru_RU:ru
ENV LC_ALL ru_RU.UTF-8