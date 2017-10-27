/*
 * Copyright (c) 2011, Chad Rockey
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Android Sensors Driver nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package udel.rpng.sensors_driver.publishers.images3;

import android.hardware.Camera;

import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfInt;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.ros.internal.message.MessageBuffers;
import org.ros.message.Time;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;

import cv_bridge.CvImage;
import sensor_msgs.CompressedImage;
import sensor_msgs.Image;
import sensor_msgs.ImageEncodings;


/**
 * @author chadrockey@gmail.com (Chad Rockey)
 * @author axelfurlan@gmail.com (Axel Furlan)
 * @author tal.regev@gmail.com  (Tal Regev)
 */
public class CameraPublisher implements NodeMain, CvCameraViewListener2 {
    protected ConnectedNode node = null;
    protected Publisher<CompressedImage> imagePublisher;
    protected Publisher<sensor_msgs.Image> rawImagePublisher;
    protected Publisher<sensor_msgs.CameraInfo> cameraInfoPublisher;
    protected sensor_msgs.CameraInfo cameraInfo;
    @SuppressWarnings("deprecation")
    protected Camera.CameraInfo info = new Camera.CameraInfo();

    private int camera_id;
    private String robotName;
    private ImageParams.ViewMode viewMode;
    private ImageParams.CompressionLevel compressionLevel;

    private int count = 0;
    private int skip = 5;

    public CameraPublisher(int camera_id, String robotName, ImageParams.ViewMode viewMode, ImageParams.CompressionLevel compressionLevel) {
        this.camera_id = camera_id;
        this.robotName = robotName;
        this.viewMode = viewMode;
        this.compressionLevel = compressionLevel;
    }


    public GraphName getDefaultNodeName() {
        return GraphName.of("android_camera_driver/cameraPublisher");
    }

    public void onError(Node node, Throwable throwable) {
    }

    public void onStart(final ConnectedNode node) {
        this.node = node;
        this.imagePublisher = node.newPublisher("/android/"+robotName+"/camera_"+(camera_id+1)+"/image/compressed", sensor_msgs.CompressedImage._TYPE);
        this.cameraInfoPublisher = node.newPublisher("/android/"+robotName+"/camera_"+(camera_id+1)+"/camera_info", sensor_msgs.CameraInfo._TYPE);
        this.rawImagePublisher = node.newPublisher("/android/" + robotName + "/camera_" + (camera_id + 1) + "/image_raw", sensor_msgs.Image._TYPE);
        this.rawImagePublisher.setLatchMode(false);
    }

    @Override
    public void onShutdown(Node arg0) {
        imagePublisher.shutdown();
        cameraInfoPublisher.shutdown();
        rawImagePublisher.shutdown();
    }

    @Override
    public void onShutdownComplete(Node arg0) {
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
    }

    @Override
    public void onCameraViewStopped() {
    }


    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        Mat         frame       = null;
        Mat         frameToSend = new Mat();
        MatOfByte   buf         = new MatOfByte();
        MatOfInt    params;

        ChannelBufferOutputStream stream = new ChannelBufferOutputStream(MessageBuffers.dynamicBuffer());
        // Get the frame data based on our view mode type
        switch (viewMode) {
            case RGBA:
                frame = inputFrame.rgba();
                break;
            case GRAY:
            case CANNY:
                frame = inputFrame.gray();
                break;
        }
        // If we are not a node yet, we are not fully initialized
        if (null != node && null != frame) {

            //noinspection deprecation
            android.hardware.Camera.getCameraInfo(camera_id, info);

            switch (viewMode) {
                case RGBA:
                    Imgproc.cvtColor(frame, frameToSend, Imgproc.COLOR_RGBA2BGR);
                    break;
                case GRAY:
                    frameToSend = frame;
                    break;
                case CANNY:
                    Imgproc.Canny(frame, frame, 80, 100);
                    frameToSend = frame;
                    break;
            }

            Time currentTime = node.getCurrentTime();

            int cols = frame.cols();
            int rows = frame.rows();

            // Lets try publishing our messages
            try {
                cameraInfo = cameraInfoPublisher.newMessage();
                cameraInfo.getHeader().setFrameId("camera");
                cameraInfo.getHeader().setStamp(currentTime);
                cameraInfo.setWidth(cols);
                cameraInfo.setHeight(rows);
                cameraInfoPublisher.publish(cameraInfo);


                if (compressionLevel != ImageParams.CompressionLevel.NONE)
                {
                    //Compressed image
                    CompressedImage image = imagePublisher.newMessage();

                    image.getHeader().setStamp(currentTime);
                    image.getHeader().setFrameId("camera");

                    image.setFormat("jpeg");
                    params = new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, compressionLevel.getLevel());
                    Imgcodecs.imencode(".jpg", frameToSend, buf, params);

                    stream.write(buf.toArray());
                    image.setData(stream.buffer().copy());
                    imagePublisher.publish(image);
                }
                else
                {
                    // Raw image
                    Image rawImage = rawImagePublisher.newMessage();
                    CvImage cvImage = null;
                    // Proccess per type
                    switch (viewMode) {
                        case RGBA:
                            cvImage = new CvImage(rawImage.getHeader(), ImageEncodings.RGB8, frameToSend);
                            break;
                        case GRAY:
                        case CANNY:
                            cvImage = new CvImage(rawImage.getHeader(), ImageEncodings.MONO8, frameToSend);
                            break;
                    }
                    // Set headers
                    cvImage.header.setFrameId("camera");
                    cvImage.header.setStamp(currentTime);
                    // Move forward in time
                    count++;
                    // See if we should publish
                    if(count > skip){
                        // Publish
                        rawImagePublisher.publish(cvImage.toImageMsg(rawImage));
                        // Reset
                        count = 0;
                    }

                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        return frame;
    }
}