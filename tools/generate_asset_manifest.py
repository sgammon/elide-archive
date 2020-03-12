#!python

import sys
import json

out = sys.stdout
allowed_bases = (
    'src', 'java', 'javatests', 'tests', 'js', 'python', 'app')


def trim_path(path):

    """Trim the output prefix from paths."""

    segments = path.split("/")
    any_bases = list(filter(lambda x: x in segments, allowed_bases))
    if len(any_bases) == 0:
        print("Failed to resolve base trim for path '%s'." % path)
        sys.exit(1)
    else:
        base_index = segments.index(any_bases[0])
        return "/".join(segments[base_index+1:])


class BaseAsset(object):

    """Abstract base class for asset objects."""

    def __init__(self, name, sources):

        """Initialize a new asset module.

           :param name: Asset module name.
           :param sources: List of sources for this module."""

        self.module = name
        self.sources = [trim_path(i) for i in sources if not i.endswith(".map")]
        self.maps = [trim_path(i) for i in sources if i.endswith(".map")]

    def serialize(self):

        """Serialize into a JSON-compatible structure."""

        return {
            "sources": self.sources,
            "maps": self.maps
        }


class JSModule(BaseAsset):

    """JavaScript module associated with an application."""

    def __init__(self, entrypoint, sources):

        """Initialize a new JS module.

           :param entrypoint: JS module entrypoint.
           :param sources: List of sources for this module."""

        super().__init__(entrypoint, sources)


class StyleModule(BaseAsset):

    """CSS module associated with an application."""

    def __init__(self, module, sources):

        """Initialize a new JS module.

           :param module: GSS module entrypoint.
           :param sources: List of sources for this module."""

        self.module = module
        self.rewrite_map = [trim_path(i) for i in sources if i.endswith(".css.js")][0]
        self.sources = [trim_path(i) for i in sources if not i.endswith(".map") and not i.endswith(".js")]
        self.maps = [trim_path(i) for i in sources if i.endswith(".map")]

    def serialize(self):

        """Serialize into a JSON-compatible structure."""

        return {
            "sources": self.sources,
            "maps": self.maps,
            "rewrite": self.rewrite_map
        }


class AssetManifest(object):

    """Holds details for a given app's asset manifest."""

    def __init__(self):

        """Initialize a new asset manifest."""

        self.js = {}
        self.css = {}
        self.rewrite_map = {}

    def add_script(self, script):

        """Add a script output.

          :param script: `JSModule` object to add to this manifest.
          :returns: Self, for chain-ability."""

        self.js[script.module] = script
        return self

    def add_style(self, style):

        """Add a style output.

          :param style: `StyleModule` object to add to this manifest.
          :returns: Self, for chain-ability."""

        self.css[style.module] = style
        return self

    def serialize(self):

        """Serialize into a JSON-compatible structure."""

        return {
            "type": "gust.assets",
            "version": 1,
            "payload": {
                "js": dict([(mod, js.serialize()) for (mod, js) in self.js.items()]),
                "css": dict([(mod, css.serialize()) for (mod, css) in self.css.items()]),
                "images": {}
            }
        }


def generate_asset_manifest(args):

    """Generate an asset manifest, which we output to `stdout`, from the
       provided input parameters."""

    last_seen_flag = None
    last_seen_module = None
    inputs = args[args.index("--") + 1:][:]
    sources_for_set = []
    manifest = AssetManifest()
    for arg in inputs:
        if arg.startswith("--js") or arg.startswith("--css"):
            # firstly, flush any block we have.
            if last_seen_module is not None:
                if last_seen_flag == "js":
                    manifest.add_script(JSModule(last_seen_module, sources_for_set))
                elif last_seen_flag == "css":
                    manifest.add_style(StyleModule(last_seen_module, sources_for_set))
                last_seen_module = None

            # we are beginning a new JS or CSS block
            last_seen_flag = (arg.startswith("--js") and "js") or "css"
            module_path, main_sourcepath = tuple(arg.split(":"))
            last_seen_module = module_path.split("=")[1]
            sources_for_set = [main_sourcepath]

        elif last_seen_flag is None or last_seen_module is None:
            print("ERROR! Failed to resolve type or module for asset at arg '%s'." % arg)
            sys.exit(1)

        else:
            sources_for_set.append(arg)

    if last_seen_module is not None:
        if last_seen_flag == "js":
            manifest.add_script(JSModule(last_seen_module, sources_for_set))
        elif last_seen_flag == "css":
            manifest.add_style(StyleModule(last_seen_module, sources_for_set))

    out.write(json.dumps(manifest.serialize(),
                         sort_keys=True,
                         indent=4,
                         separators=(',', ': ')))


if __name__ == "__main__":
    generate_asset_manifest(sys.argv)
