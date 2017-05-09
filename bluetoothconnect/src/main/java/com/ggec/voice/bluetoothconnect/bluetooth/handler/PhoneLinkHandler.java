//package com.ggec.voice.bluetoothconnect.bluetooth.handler;
//
//import android.util.Log;
//import android.util.SparseArray;
//
//
//import java.nio.ByteBuffer;
//import java.util.concurrent.Callable;
//import java.util.concurrent.TimeUnit;
//
//import io.reactivex.Observable;
//import io.reactivex.android.schedulers.AndroidSchedulers;
//import io.reactivex.functions.Consumer;
//import io.reactivex.schedulers.Schedulers;
//
///**
// * Created by ggec on 2017/5/9.
// */
//
//public class PhoneLinkHandler extends LinkHandler {
//    private final SeqIdGenerator mSeqGenerator;
//    final SparseArray<Object> mSendList = new SparseArray<>();
//
//    public PhoneLinkHandler() {
//        super();
//
//        mSeqGenerator = new SeqIdGenerator();
//    }
//
//    @Override
//    protected void sendData(IProtocol protocol) {
//        super.sendData(protocol);
//    }
//
//    @Override
//    public void onDataImpl(int uri, ByteBuffer data, boolean skipHead) {
//        if (uri == ProtoURI.GetDeviceInfoResURI) {
//            GetDeviceInfoRes receive = new GetDeviceInfoRes();
//            try {
//                receive.unMarshall(data);
//                handleGetDeviceInfoRes(receive);
//            } catch (InvalidProtocolData invalidProtocolData) {
//                invalidProtocolData.printStackTrace();
//            }
//
//        } else if (uri == ProtoURI.SendAuth2DeviceAckURI) {
//            SendAuth2DeviceAck receive = new SendAuth2DeviceAck();
//            try {
//                receive.unMarshall(data);
//                handleSendAuth2DeviceAck(receive);
//            } catch (InvalidProtocolData invalidProtocolData) {
//                invalidProtocolData.printStackTrace();
//            }
//        } else if (uri == ProtoURI.GetDeviceWifiScansResURI) {
//            GetDeviceWifiScanRes receive = new GetDeviceWifiScanRes();
//            try {
//                receive.unMarshall(data);
//                handleGetDeviceWifiScanRes(receive);
//            } catch (InvalidProtocolData invalidProtocolData) {
//                invalidProtocolData.printStackTrace();
//            }
//        } else if (uri == ProtoURI.SendWifiConfig2DeviceResURI) {
//
//            SendWifiConfig2DeviceAck receive = new SendWifiConfig2DeviceAck();
//            try {
//                receive.unMarshall(data);
//                handleSendWifiConfig2DeviceRes(receive);
//            } catch (InvalidProtocolData invalidProtocolData) {
//                invalidProtocolData.printStackTrace();
//            }
//        }
//    }
//
//
//    public void getDeviceInfo(final IGetDeviceInfoCallback callback) {
//        Observable.fromCallable(new Callable<IProtocol>() {
//            @Override
//            public IProtocol call() throws Exception {
//                GetDeviceInfoReq req = new GetDeviceInfoReq();
//                req.seqId = mSeqGenerator.nextSeqId();
//
//                mSendList.put(req.seq(), callback);
//                Log.d(TAG, "getDeviceInfo start");
//                sendData(req);
//                return req;
//            }
//        }).subscribeOn(Schedulers.io())
//                .delay(ProtoResult.TIME_OUT_INTERVAL, TimeUnit.SECONDS)
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(new Consumer<IProtocol>() {
//                    @Override
//                    public void accept(IProtocol protocol) throws Exception {
//                        if (null != mSendList.get(protocol.seq())) {
//                            Object object = mSendList.get(protocol.seq());
//                            mSendList.remove(protocol.seq());
//                            if (object instanceof IGetDeviceInfoCallback) {
//                                ((IGetDeviceInfoCallback) object).onFail(ProtoResult.TIME_OUT);
//                            }
//                        }
//                    }
//                });
//    }
//
//    private void handleGetDeviceInfoRes(GetDeviceInfoRes receive) {
//        Log.d(TAG, "handleGetDeviceInfoRes " + receive.resCode + " msg:" + receive.message + " seq:" + receive.seqId
//                + " product:" + receive.productId + "\ndsn:" + receive.deviceSerialNumber);
//        Object listener = mSendList.get(receive.seqId);
//        mSendList.remove(receive.seq());
//        if (listener instanceof IGetDeviceInfoCallback) {
//            if (receive.resCode == ProtoResult.SUCCESS) {
//                ((IGetDeviceInfoCallback) listener).onSuccess(receive.productId, receive.deviceSerialNumber);
//            } else {
//                ((IGetDeviceInfoCallback) listener).onFail(receive.resCode);
//            }
//        }
//    }
//
//    public void getDeviceWifiScan(final IGetDeviceWifiScansCallback callback) {
//        Observable.fromCallable(new Callable<IProtocol>() {
//            @Override
//            public IProtocol call() throws Exception {
//                GetDeviceWifiScanReq req = new GetDeviceWifiScanReq();
//                req.seqId = mSeqGenerator.nextSeqId();
//
//                ProtoQueueObject protoQueueObject = new ProtoQueueObject(req, callback);
//                mSendList.put(req.seq(), protoQueueObject);
//                Log.d(TAG, "getDeviceWifiScan start");
//                sendData(req);
//                return req;
//            }
//        }).subscribeOn(Schedulers.io())
//                .delay(ProtoResult.TIME_OUT_INTERVAL, TimeUnit.SECONDS)
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(new Consumer<IProtocol>() {
//                    @Override
//                    public void accept(IProtocol protocol) throws Exception {
//                        if (null != mSendList.get(protocol.seq())) {
//                            Object mListener = mSendList.get(protocol.seq());
//                            mSendList.remove(protocol.seq());
//                            if (mListener instanceof IGetDeviceWifiScansCallback) {
//                                ((IGetDeviceWifiScansCallback) mListener).onFail(ProtoResult.TIME_OUT);
//                            }
//                        }
//                    }
//                });
//    }
//
//    private void handleGetDeviceWifiScanRes(GetDeviceWifiScanRes receive) {
//        Log.d(TAG, "handleGetDeviceWifiScanRes " + receive.resCode + " msg:" + receive.message
//                + " seq:" + receive.seqId + " size:" + receive.data.size());
//        Object listener = mSendList.get(receive.seqId);
//        mSendList.remove(receive.seq());
//        if (listener instanceof IGetDeviceWifiScansCallback) {
//            if (receive.resCode == ProtoResult.SUCCESS) {
//                ((IGetDeviceWifiScansCallback) listener).onSuccess(receive.data);
//            } else {
//                ((IGetDeviceWifiScansCallback) listener).onFail(receive.resCode);
//            }
//        }
//    }
//
//    private void handleSendWifiConfig2DeviceRes(SendWifiConfig2DeviceAck receive) {
//        Log.d(TAG, "handleSendWifiConfig2DeviceRes " + receive.resCode + " msg:" + receive.message
//                + " seq:" + receive.seqId);
//        Object listener = mSendList.get(receive.seqId);
//        mSendList.remove(receive.seq());
//        if (listener instanceof ICommonCallback) {
//            if (receive.resCode == ProtoResult.SUCCESS) {
//                ((ICommonCallback) listener).onSuccess();
//            } else {
//                ((ICommonCallback) listener).onFail(receive.resCode);
//            }
//        }
//    }
//
//    public void sendWifiConfig2Device(final String ssid, final String password, final ICommonCallback callback) {
//        Observable.fromCallable(new Callable<IProtocol>() {
//            @Override
//            public IProtocol call() throws Exception {
//                SendWifiConfig2DeviceReq req = new SendWifiConfig2DeviceReq();
//                req.seqId = mSeqGenerator.nextSeqId();
//                req.password = password;
//                req.ssid = ssid;
//
//                ProtoQueueObject protoQueueObject = new ProtoQueueObject(req, callback);
//                mSendList.put(req.seq(), protoQueueObject);
//                Log.d(TAG, "SendWifiConfig2Device start");
//                sendData(req);
//                return req;
//            }
//        }).subscribeOn(Schedulers.io())
//                .delay(ProtoResult.TIME_OUT_INTERVAL, TimeUnit.SECONDS)
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(new Consumer<IProtocol>() {
//                    @Override
//                    public void accept(IProtocol protocol) throws Exception {
//                        if (null != mSendList.get(protocol.seq())) {
//                            Object mListener = mSendList.get(protocol.seq());
//                            mSendList.remove(protocol.seq());
//                            if (mListener instanceof ICommonCallback) {
//                                ((ICommonCallback) mListener).onFail(ProtoResult.TIME_OUT);
//                            }
//                        }
//                    }
//                });
//    }
//
//    private void handleSendAuth2DeviceAck(SendAuth2DeviceAck receive) {
//        Log.d(TAG, "handleSendAuth2DeviceAck " + receive.resCode + " msg:" + receive.message
//                + " seq:" + receive.seqId);
//        Object listener = mSendList.get(receive.seqId);
//        mSendList.remove(receive.seq());
//        if (listener instanceof ICommonCallback) {
//            if (receive.resCode == ProtoResult.SUCCESS) {
//                ((ICommonCallback) listener).onSuccess();
//            } else {
//                ((ICommonCallback) listener).onFail(receive.resCode);
//            }
//        }
//    }
//
//    public void sendAuth2Device(final String authCode, final ICommonCallback callback) {
//        Observable.fromCallable(new Callable<IProtocol>() {
//            @Override
//            public IProtocol call() throws Exception {
//                SendAuth2DeviceReq req = new SendAuth2DeviceReq();
//                req.seqId = mSeqGenerator.nextSeqId();
//                req.authCode = authCode;
//
//                ProtoQueueObject protoQueueObject = new ProtoQueueObject(req, callback);
//                mSendList.put(req.seq(), protoQueueObject);
//                Log.d(TAG, "sendAuth2Device start");
//                sendData(req);
//                return req;
//            }
//        }).subscribeOn(Schedulers.io())
//                .delay(ProtoResult.TIME_OUT_INTERVAL, TimeUnit.SECONDS)
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(new Consumer<IProtocol>() {
//                    @Override
//                    public void accept(IProtocol protocol) throws Exception {
//                        if (null != mSendList.get(protocol.seq())) {
//                            Object mListener = mSendList.get(protocol.seq());
//                            mSendList.remove(protocol.seq());
//                            if (mListener instanceof ICommonCallback) {
//                                ((ICommonCallback) mListener).onFail(ProtoResult.TIME_OUT);
//                            }
//                        }
//                    }
//                });
//    }
//}
