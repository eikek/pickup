with import <nixpkgs> { };

# A FHS env to create debian packages on NixOS
#
# Run `nix-build build.nix` and then start sbt via `./result/bin/pickup-build -c sbt`
#

buildFHSUserEnv {
  name = "pickup-build";
  targetPkgs = pkgs: with pkgs; [
    netcat jdk8 wget which zsh dpkg sbt git elmPackages.elm ncurses fakeroot mc jekyll
    # haskells http client needs this (to download elm packages)
    iana-etc
  ];
  runScript = "$SHELL";
}
