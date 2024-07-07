{
  description = "Dev shell flake for fcitx5-android-updater";

  inputs.fcitx5-android.url = "github:fcitx5-android/fcitx5-android";
  inputs.flake-compat = {
    url = "github:edolstra/flake-compat";
    flake = false;
  };

  outputs = { self, fcitx5-android, ... }:
    let
      nixpkgs = fcitx5-android.inputs.nixpkgs;
      pkgs = import nixpkgs {
        system = "x86_64-linux";
        config.android_sdk.accept_license = true;
        config.allowUnfree = true;
        overlays = [ fcitx5-android.overlays.default ];
      };
    in with pkgs;
    let sdk = pkgs.fcitx5-android.sdk;
    in { devShells.x86_64-linux.default = sdk.shell; };
}
