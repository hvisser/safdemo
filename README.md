This is an small example to demonstrate the features of the Storage Access Framework.

Much of it is based on the nice guide on [developer.android.com][1], which I recommend you read.

The Vault directory contains a non-trivial example DocumentsProvider by [Jeff Sharkey][2] that encrypts files, copied from [the Android source code][3].
I've added a build.gradle for easy consumption. Other examples include [Storage Provider][5] and [Storage Client][6].

Another nice resource on SAF is [episode 2 of Android Developers Backstage][4] so give that a listen while you're at it too.

[1]:https://developer.android.com/guide/topics/providers/document-provider.html
[2]:https://plus.google.com/+JeffSharkey/posts/9BmGb3xbPcA
[3]:https://android.googlesource.com/platform/development/+/master/samples/Vault/
[4]:https://plus.google.com/+AndroidDevelopers/posts/XU2R399y54g
[5]:https://developer.android.com/samples/StorageProvider/index.html
[6]:https://developer.android.com/samples/StorageClient/index.html