var _user$project$Native_Binary_ArrayBuffer = function() {

function _new(length) {
  return new ArrayBuffer(length);
}

function byteLength(buffer) {
  return buffer.byteLength;
}

function stringToBufferArray(str, idxStart, idxEnd) {
  var buf = new ArrayBuffer(str.length*2); // 2 bytes for each char
  var bufView = new Uint16Array(buf);
  for (var i = idxStart || 0, end = idxEnd || str.length; i < end; i++) {
    bufView[i] = str.charCodeAt(i);
  }
  return buf;
}

function asUint8Array(buffer) {
  return new Uint8Array(buffer);
}

function getByte(buffer, index) {
  return buffer[index];
}

function setByte(buffer, index, value) {
  buffer[index] = value;
  return buffer;
}

return {
  _new: _new,
  byteLength: byteLength,
  stringToBufferArray: stringToBufferArray,
  stringToBufferArrayWithOffset: F3(stringToBufferArray),
  asUint8Array: asUint8Array,
  getByte: F2(getByte),
  setByte: F3(setByte)
}

}()
/* SHIM Native.BinaryArrayBuffer */