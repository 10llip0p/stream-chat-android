## Common changes for all artifacts
### 🐞 Fixed

### ⬆️ Improved

### ✅ Added

### ⚠️ Changed

### ❌ Removed


## stream-chat-android
### 🐞 Fixed

### ⬆️ Improved

### ✅ Added

### ⚠️ Changed

### ❌ Removed

## stream-chat-android-client
### 🐞 Fixed
Fixed the usage of `ProgressCallback` in `ChannelClient::sendFile` and `ChannelClient::sendImage` methods.
### ⬆️ Improved

### ✅ Added
Added `ChannelClient::deleteFile` and `ChannelClient::deleteImage` methods.
### ⚠️ Changed

### ❌ Removed

## stream-chat-android-offline
### 🐞 Fixed

### ⬆️ Improved

### ✅ Added

### ⚠️ Changed
- Changed the upload logic in `ChannelController` for the images unsupported by the Stream CDN. Now such images are uploaded as files via `ChannelClient::sendFile` method.
### ❌ Removed

## stream-chat-android-ui-common
### 🐞 Fixed

### ⬆️ Improved

### ✅ Added

### ⚠️ Changed
- Deprecated `MessageInputViewModel::editMessage`. Use `MessageInputViewModel::messageToEdit` and `MessageInputViewModel::postMessageToEdit` instead.
- Changed `MessageInputViewModel::repliedMessage` type to `LiveData`. Use `ChatDomain::setMessageForReply` for setting message for reply.
- Changed `MessageListViewModel::mode` type to `LiveData`. Mode is handled internally and shouldn't be modified outside the SDK.

### ❌ Removed

## stream-chat-android-ui-components
### 🐞 Fixed
- Removed empty badge for selected media attachments.
### ⬆️ Improved

### ✅ Added
- Added `messageLimit` argument to `ChannelListViewModel` and `ChannelListViewModelFactory` constructors to allow changing the number of fetched messages for each channel in the channel list.
### ⚠️ Changed

### ❌ Removed
