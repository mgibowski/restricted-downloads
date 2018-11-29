#restricted-downloads

Initial requirements:

This app should allow downloading files restricted by code.

Characteristics of a `DownloadableFile`:
- it has expiry date, after which it can not be downloaded
- in order to download a file, a user needs to provide a `DownloadCode`
- Each `DownloadCode` can be used up to 5 times

