{ pkgs ? import <nixpkgs> {} }:

let
  runtimeLibs = with pkgs; [
    stdenv.cc.cc.lib
    zlib
    glib
    libGL
    libxkbcommon
    xorg.libxcb
    xorg.libX11
    xorg.libXext
    xorg.libXrender
    xorg.libXi
    xorg.libXfixes
    libpng
    libjpeg
  ];
in

pkgs.mkShell {
  buildInputs = with pkgs; [
    python313
    python313Packages.pip
    python313Packages.virtualenv
    python313Packages.setuptools
  ] ++ runtimeLibs;

  shellHook = ''
    export LD_LIBRARY_PATH=${pkgs.lib.makeLibraryPath runtimeLibs}:''${LD_LIBRARY_PATH:-}

    if [ ! -d .venv-nix ]; then
      echo "Creating Nix-compatible virtualenv (.venv-nix)..."
      python3 -m venv .venv-nix
      source .venv-nix/bin/activate
      pip install --upgrade pip
      pip install -r requirements.txt
    else
      source .venv-nix/bin/activate
    fi

    echo "Pose analyzer shell ready (Python $(python --version 2>&1))."
  '';
}
