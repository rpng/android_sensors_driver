/*
 * Copyright (C) 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package udel.rpng.sensors_driver.publishers.images;

import com.google.common.base.Preconditions;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera.Size;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.ros.internal.message.MessageBuffers;
import org.ros.message.Time;
import org.ros.namespace.NameResolver;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

import java.io.ByteArrayInputStream;

/**
 * Publishes preview framesm in raw format.
 *
 * @author
 */
public class RawImagePublisher implements RawImageListener {

    private final ConnectedNode connectedNode;
    private final Publisher<sensor_msgs.Image> imagePublisher;

    private String robotName;
    private int camera_id;

    private byte[] rawImageBuffer;
    private Size rawImageSize;
    private YuvImage yuvImage;
    private Rect rect;
    private ChannelBufferOutputStream stream;

    public RawImagePublisher(ConnectedNode connectedNode, String robotName, int camera_id) {
        this.connectedNode = connectedNode;
        this.robotName = robotName;
        this.camera_id = camera_id;
        imagePublisher = connectedNode.newPublisher("/android/"+robotName+"/camera_"+camera_id+"/image/raw", sensor_msgs.Image._TYPE);
        stream = new ChannelBufferOutputStream(MessageBuffers.dynamicBuffer());
    }

    @Override
    public void onNewRawImage(byte[] data, Size size) {
        Preconditions.checkNotNull(data);
        Preconditions.checkNotNull(size);
        if (data != rawImageBuffer || !size.equals(rawImageSize)) {
            rawImageBuffer = data;
            rawImageSize = size;
            yuvImage = new YuvImage(rawImageBuffer, ImageFormat.NV21, size.width, size.height, null);
            rect = new Rect(0, 0, size.width, size.height);
        }
        Time currentTime = connectedNode.getCurrentTime();
        String frameId = "/android/camera_"+camera_id;
        sensor_msgs.Image image = imagePublisher.newMessage();
        image.setEncoding("yuv422");
        image.setHeight(size.height);
        image.setWidth(size.width);
        image.getHeader().setStamp(currentTime);
        image.getHeader().setFrameId(frameId);

        Preconditions.checkState(yuvImage.compressToJpeg(rect, 80, stream));
        image.setData(stream.buffer().copy());
        stream.buffer().clear();

        imagePublisher.publish(image);
    }
}