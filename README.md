# nanojson

This is a fork of [nanojson](https://github.com/mmastrac/nanojson) for use by NewPipe(Extractor). It has the following changes:

- It returns an empty `JsonObject` or `JsonArray` by default instead of `null`, preventing `NullPointerException`s.
- It accepts JS-like JSON, such as `{ this: 'is', an: 'example' }`.