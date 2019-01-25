# ImitateBitmap 模仿者
## 1，可伸缩容器 DynScaleBitmapContainer
因为ArrayContainer在4096个short的时候会直接转化成BitmapContainer，
假设上ArrayContainer上的数据是相对连续的（当然RoaringBitmap还有一个行程压缩的RunBitmap），在当它满足4096个转成Bitmap的时候，BitmapContainer是有一大部分的空间是浪费的，DynScaleBitmapContainer保证了一定的压缩率以及查询性能

## 2，堆外内存容器 DirectByteBufferContainer
一个bitmap内存占用从几K到几百M，如果bitmap做为常驻堆内存中提供服务的时候无可避免会对JVM的GC造成一定的压力，DirectByteBufferContainer将会暂缓GC的问题

## 3，文件容器 FileBitmapContainer.java
当使用bitmap提供服务常驻内存却又是冷数据时，将bitmap从内存中转换成文件bitmap是最为合适了，减少了内存压力同时提供可接受的服务性能

## 3，一次性写入性能优化
imitaebitmap是参照roaringbitmap的分桶做的，但是结合自己的场景，bitmap都是一次性写入完然后提供服务，然鹅原先的bitmap是类似于arraylist不设定初始化长度一般慢慢扩容，中间会造成许多的过渡数据碎片产生，无论是对写入性能以及GC来看都是影响巨大，本次优化如同设定arraylist初始化长度一般，优化性能从原来随机一亿写入77秒到22秒的提升
