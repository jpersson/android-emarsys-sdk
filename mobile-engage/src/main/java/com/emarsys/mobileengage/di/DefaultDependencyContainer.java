package com.emarsys.mobileengage.di;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.emarsys.core.DeviceInfo;
import com.emarsys.core.activity.ActivityLifecycleAction;
import com.emarsys.core.activity.ActivityLifecycleWatchdog;
import com.emarsys.core.concurrency.CoreSdkHandlerProvider;
import com.emarsys.core.connection.ConnectionWatchDog;
import com.emarsys.core.database.helper.CoreDbHelper;
import com.emarsys.core.database.repository.Repository;
import com.emarsys.core.database.repository.SqlSpecification;
import com.emarsys.core.database.repository.log.LogRepository;
import com.emarsys.core.database.trigger.TriggerKey;
import com.emarsys.core.provider.timestamp.TimestampProvider;
import com.emarsys.core.provider.uuid.UUIDProvider;
import com.emarsys.core.request.RequestManager;
import com.emarsys.core.request.RestClient;
import com.emarsys.core.request.model.RequestModel;
import com.emarsys.core.request.model.RequestModelRepository;
import com.emarsys.core.shard.ShardModel;
import com.emarsys.core.shard.ShardModelRepository;
import com.emarsys.core.worker.DefaultWorker;
import com.emarsys.core.worker.Worker;
import com.emarsys.mobileengage.MobileEngageCoreCompletionHandler;
import com.emarsys.mobileengage.MobileEngageInternal;
import com.emarsys.mobileengage.MobileEngageStatusListener;
import com.emarsys.mobileengage.RequestContext;
import com.emarsys.mobileengage.config.MobileEngageConfig;
import com.emarsys.mobileengage.database.MobileEngageDbHelper;
import com.emarsys.mobileengage.deeplink.DeepLinkAction;
import com.emarsys.mobileengage.deeplink.DeepLinkInternal;
import com.emarsys.mobileengage.experimental.MobileEngageExperimental;
import com.emarsys.mobileengage.experimental.MobileEngageFeature;
import com.emarsys.mobileengage.iam.DoNotDisturbProvider;
import com.emarsys.mobileengage.iam.InAppPresenter;
import com.emarsys.mobileengage.iam.InAppStartAction;
import com.emarsys.mobileengage.iam.dialog.IamDialogProvider;
import com.emarsys.mobileengage.iam.jsbridge.InAppMessageHandlerProvider;
import com.emarsys.mobileengage.iam.model.buttonclicked.ButtonClickedRepository;
import com.emarsys.mobileengage.iam.model.displayediam.DisplayedIamRepository;
import com.emarsys.mobileengage.iam.model.requestRepositoryProxy.RequestRepositoryProxy;
import com.emarsys.mobileengage.iam.webview.IamWebViewProvider;
import com.emarsys.mobileengage.inbox.InboxInternal;
import com.emarsys.mobileengage.inbox.InboxInternalProvider;
import com.emarsys.mobileengage.log.LogRepositoryProxy;
import com.emarsys.mobileengage.log.handler.IamMetricsLogHandler;
import com.emarsys.mobileengage.responsehandler.AbstractResponseHandler;
import com.emarsys.mobileengage.responsehandler.InAppCleanUpResponseHandler;
import com.emarsys.mobileengage.responsehandler.InAppMessageResponseHandler;
import com.emarsys.mobileengage.responsehandler.MeIdResponseHandler;
import com.emarsys.mobileengage.storage.AppLoginStorage;
import com.emarsys.mobileengage.storage.MeIdSignatureStorage;
import com.emarsys.mobileengage.storage.MeIdStorage;
import com.emarsys.mobileengage.util.RequestHeaderUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultDependencyContainer implements DependencyContainer {

    private MobileEngageInternal mobileEngageInternal;
    private InboxInternal inboxInternal;
    private DeepLinkInternal deepLinkInternal;
    private Handler coreSdkHandler;
    private RequestContext requestContext;
    private MobileEngageCoreCompletionHandler completionHandler;

    private Handler uiHandler;
    private TimestampProvider timestampProvider;
    private DoNotDisturbProvider doNotDisturbProvider;
    private AppLoginStorage appLoginStorage;
    private MeIdStorage meIdStorage;
    private MeIdSignatureStorage meIdSignatureStorage;
    private DeviceInfo deviceInfo;
    private RequestManager requestManager;
    private ButtonClickedRepository buttonClickedRepository;
    private DisplayedIamRepository displayedIamRepository;
    private Repository<RequestModel, SqlSpecification> requestModelRepository;
    private Repository<ShardModel, SqlSpecification> shardModelRepository;
    private RestClient restClient;
    private Repository<Map<String, Object>, SqlSpecification> logRepositoryProxy;
    private Application application;
    private ActivityLifecycleWatchdog activityLifecycleWatchdog;
    private InAppPresenter inAppPresenter;
    private UUIDProvider uuidProvider;

    public DefaultDependencyContainer(MobileEngageConfig mobileEngageConfig) {
        initializeDependencies(mobileEngageConfig);
        initializeInstances();
        initializeInAppPresenter();
        initializeResponseHandlers();
        initializeActivityLifecycleWatchdog();
    }

    @Override
    public MobileEngageInternal getMobileEngageInternal() {
        return mobileEngageInternal;
    }

    @Override
    public InboxInternal getInboxInternal() {
        return inboxInternal;
    }

    @Override
    public DeepLinkInternal getDeepLinkInternal() {
        return deepLinkInternal;
    }

    @Override
    public Handler getCoreSdkHandler() {
        return coreSdkHandler;
    }

    @Override
    public RequestContext getRequestContext() {
        return requestContext;
    }

    @Override
    public MobileEngageCoreCompletionHandler getCoreCompletionHandler() {
        return completionHandler;
    }

    @Override
    public ActivityLifecycleWatchdog getActivityLifecycleWatchdog() {
        return activityLifecycleWatchdog;
    }

    @Override
    public InAppPresenter getInAppPresenter() {
        return inAppPresenter;
    }

    private void initializeDependencies(MobileEngageConfig config) {
        application = config.getApplication();

        uiHandler = new Handler(Looper.getMainLooper());
        coreSdkHandler = new CoreSdkHandlerProvider().provideHandler();
        timestampProvider = new TimestampProvider();
        uuidProvider = new UUIDProvider();
        doNotDisturbProvider = new DoNotDisturbProvider();
        appLoginStorage = new AppLoginStorage(application);
        meIdStorage = new MeIdStorage(application);
        meIdSignatureStorage = new MeIdSignatureStorage(application);
        deviceInfo = new DeviceInfo(application);
        MobileEngageDbHelper mobileEngageDbHelper = new MobileEngageDbHelper(application, new HashMap<TriggerKey, List<Runnable>>());
        buttonClickedRepository = new ButtonClickedRepository(mobileEngageDbHelper);
        displayedIamRepository = new DisplayedIamRepository(mobileEngageDbHelper);
        completionHandler = new MobileEngageCoreCompletionHandler(config.getStatusListener());

        requestModelRepository = createRequestModelRepository(application);

        CoreDbHelper coreDbHelper = new CoreDbHelper(application, new HashMap<TriggerKey, List<Runnable>>());
        shardModelRepository = new ShardModelRepository(coreDbHelper);

        Repository<Map<String, Object>, SqlSpecification> logRepository = new LogRepository(application);
        List<com.emarsys.core.handler.Handler<Map<String, Object>, Map<String, Object>>> logHandlers = Arrays.<com.emarsys.core.handler.Handler<Map<String, Object>, Map<String, Object>>>asList(
                new IamMetricsLogHandler(new HashMap<String, Map<String, Object>>())
        );
        logRepositoryProxy = new LogRepositoryProxy(logRepository, logHandlers);
        restClient = new RestClient(logRepositoryProxy, timestampProvider);

        ConnectionWatchDog connectionWatchDog = new ConnectionWatchDog(application, coreSdkHandler);
        Worker worker = new DefaultWorker(
                requestModelRepository,
                connectionWatchDog,
                uiHandler,
                coreSdkHandler,
                completionHandler,
                restClient);

        requestManager = new RequestManager(
                coreSdkHandler,
                requestModelRepository,
                shardModelRepository,
                worker,
                restClient);

        requestContext = new RequestContext(
                config.getApplicationCode(),
                config.getApplicationPassword(),
                deviceInfo,
                appLoginStorage,
                meIdStorage,
                meIdSignatureStorage,
                timestampProvider,
                uuidProvider);

        requestManager.setDefaultHeaders(RequestHeaderUtils.createDefaultHeaders(requestContext));
    }

    private Repository<RequestModel, SqlSpecification> createRequestModelRepository(Context application) {
        CoreDbHelper coreDbHelper = new CoreDbHelper(application, new HashMap<TriggerKey, List<Runnable>>());
        RequestModelRepository requestModelRepository = new RequestModelRepository(coreDbHelper);
        if (MobileEngageExperimental.isV3Enabled()) {
            return new RequestRepositoryProxy(
                    deviceInfo,
                    requestModelRepository,
                    displayedIamRepository,
                    buttonClickedRepository,
                    timestampProvider,
                    doNotDisturbProvider);
        } else {
            return requestModelRepository;
        }
    }

    private void initializeInstances() {
        mobileEngageInternal = new MobileEngageInternal(
                requestManager,
                uiHandler,
                completionHandler,
                requestContext
        );
        inboxInternal = new InboxInternalProvider().provideInboxInternal(
                MobileEngageExperimental.isFeatureEnabled(MobileEngageFeature.USER_CENTRIC_INBOX),
                requestManager,
                restClient,
                requestContext,
                new MobileEngageStatusListener() {
                    @Override
                    public void onError(String id, Exception cause) {

                    }

                    @Override
                    public void onStatusLog(String id, String log) {

                    }
                }
        );
        deepLinkInternal = new DeepLinkInternal(requestManager, requestContext);
    }

    private void initializeActivityLifecycleWatchdog() {
        ActivityLifecycleAction[] applicationStartActions = null;
        if (MobileEngageExperimental.isFeatureEnabled(MobileEngageFeature.IN_APP_MESSAGING)) {
            applicationStartActions = new ActivityLifecycleAction[]{
                    new InAppStartAction(mobileEngageInternal)
            };
        }

        ActivityLifecycleAction[] activityCreatedActions = new ActivityLifecycleAction[]{
                new DeepLinkAction(deepLinkInternal)
        };

        activityLifecycleWatchdog = new ActivityLifecycleWatchdog(
                applicationStartActions,
                activityCreatedActions);
    }

    private void initializeInAppPresenter() {
        inAppPresenter = new InAppPresenter(
                coreSdkHandler,
                new IamWebViewProvider(),
                new InAppMessageHandlerProvider(),
                new IamDialogProvider(),
                buttonClickedRepository,
                displayedIamRepository,
                timestampProvider,
                mobileEngageInternal);
    }

    private void initializeResponseHandlers() {
        List<AbstractResponseHandler> responseHandlers = new ArrayList<>();

        if (MobileEngageExperimental.isV3Enabled()) {
            responseHandlers.add(new MeIdResponseHandler(
                    meIdStorage,
                    meIdSignatureStorage));
        }

        if (MobileEngageExperimental.isFeatureEnabled(MobileEngageFeature.IN_APP_MESSAGING)) {
            responseHandlers.add(new InAppMessageResponseHandler(
                    inAppPresenter,
                    logRepositoryProxy,
                    timestampProvider));

            responseHandlers.add(new InAppCleanUpResponseHandler(
                    displayedIamRepository,
                    buttonClickedRepository
            ));
        }

        completionHandler.addResponseHandlers(responseHandlers);
    }
}
