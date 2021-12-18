FROM ubuntu:18.04
RUN apt-get update && apt-get full-upgrade -y
RUN yes | apt-get install openjdk-8-jdk
RUN yes | apt-get install wget
RUN yes | apt-get install libxss1 libgconf2-4 libappindicator1 libindicator7 fonts-liberation libappindicator3-1 lsb-release xdg-utils
RUN yes | apt-get install xvfb xfonts-100dpi xfonts-75dpi xfonts-scalable xfonts-cyrillic
#RUN wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb
#RUN yes | dpkg -i --force-depends google-chrome-stable_current_amd64.deb
RUN wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add -
RUN sh -c 'echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google-chrome.list'
RUN apt-get -y update
RUN apt-get install -y google-chrome-stable
RUN yes | apt-get install language-pack-ru
RUN yes | apt-get install tzdata
RUN yes | ln -fs /usr/share/zoneinfo/Europe/Moscow /etc/localtime && dpkg-reconfigure -f noninteractive tzdata
RUN sed -i -e 's/# ru_RU.UTF-8 UTF-8/ru_RU.UTF-8 UTF-8/' /etc/locale.gen && locale-gen
RUN apt-get install -yqq unzip
RUN wget -O /tmp/chromedriver.zip http://chromedriver.storage.googleapis.com/`curl -sS chromedriver.storage.googleapis.com/LATEST_RELEASE`/chromedriver_linux64.zip
RUN unzip /tmp/chromedriver.zip chromedriver -d /usr/local/bin/
#RUN mkdir -p /srv/parsers
#ADD parser_all-1.0-jar-with-dependencies.jar /srv/parsers/
#ADD settings.json /srv/parsers/
RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        curl \
        ca-certificates \
        \
        # .NET dependencies
        libc6 \
        libgcc1 \
        libgssapi-krb5-2 \
        libicu66 \
        libssl1.1 \
        libstdc++6 \
        zlib1g \
    && rm -rf /var/lib/apt/lists/*
RUN curl -sSL https://dot.net/v1/dotnet-install.sh | bash /dev/stdin -Channel 2.0 -Runtime dotnet -InstallDir /usr/share/dotnet \
    && ln -s /usr/share/dotnet/dotnet /usr/bin/dotnet
RUN curl -sSL https://dot.net/v1/dotnet-install.sh | bash /dev/stdin -Channel 3.0 -Runtime dotnet -InstallDir /usr/share/dotnet \
    && ln -s /usr/share/dotnet/dotnet /usr/bin/dotnet
RUN curl -sSL https://dot.net/v1/dotnet-install.sh | bash /dev/stdin -Channel 5.0 -Runtime dotnet -InstallDir /usr/share/dotnet \
    && ln -s /usr/share/dotnet/dotnet /usr/bin/dotnet
RUN touch /etc/default/locale
RUN echo LANG="ru_RU.UTF-8" >> /etc/default/locale
RUN echo LC_ALL="ru_RU.UTF-8" >> /etc/default/locale
RUN apt-get clean
ENV LANG ru_RU.UTF-8
ENV LANGUAGE ru_RU:ru
ENV LC_ALL ru_RU.UTF-8