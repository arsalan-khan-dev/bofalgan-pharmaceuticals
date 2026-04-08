package com.bofalgan.pharmacy.config;

import com.bofalgan.pharmacy.dao.*;
import com.bofalgan.pharmacy.db.DatabaseManager;
import com.bofalgan.pharmacy.db.SchemaInitializer;
import com.bofalgan.pharmacy.service.*;
import com.bofalgan.pharmacy.storage.FileStorageManager;

/**
 * Central application context / service locator.
 * Initialized once at startup; all controllers pull services from here.
 */
public class AppContext {

    private static AppContext instance;

    // DAO layer
    private final MedicineDAO    medicineDAO;
    private final UserDAO        userDAO;
    private final SupplierDAO    supplierDAO;
    private final InvoiceDAO     invoiceDAO;
    private final PurchaseDAO    purchaseDAO;
    private final ActivityLogDAO activityLogDAO;
    private final AlertDAO       alertDAO;
    private final AnalyticsDAO   analyticsDAO;

    // Service layer
    private final AuthService     authService;
    private final MedicineService medicineService;
    private final InvoiceService  invoiceService;
    private final PurchaseService purchaseService;
    private final UserService     userService;
    private final SupplierService supplierService;

    private AppContext() {
        DatabaseManager db = DatabaseManager.getInstance();

        // Initialize DAOs
        this.medicineDAO    = new MedicineDAO(db);
        this.userDAO        = new UserDAO(db);
        this.supplierDAO    = new SupplierDAO(db);
        this.invoiceDAO     = new InvoiceDAO(db);
        this.purchaseDAO    = new PurchaseDAO(db);
        this.activityLogDAO = new ActivityLogDAO(db);
        this.alertDAO       = new AlertDAO(db);
        this.analyticsDAO   = new AnalyticsDAO(db);

        // Initialize services
        this.authService     = new AuthService(userDAO, activityLogDAO);
        this.medicineService = new MedicineService(medicineDAO, alertDAO, activityLogDAO);
        this.invoiceService  = new InvoiceService(invoiceDAO, medicineDAO, activityLogDAO);
        this.purchaseService = new PurchaseService(purchaseDAO, medicineDAO, activityLogDAO);
        this.userService     = new UserService(userDAO, activityLogDAO);
        this.supplierService = new SupplierService(supplierDAO, activityLogDAO);
    }

    public static synchronized AppContext getInstance() {
        if (instance == null) instance = new AppContext();
        return instance;
    }

    public static void initialize() {
        DatabaseManager db = DatabaseManager.getInstance();
        db.initialize();
        new SchemaInitializer(db).initialize();
        FileStorageManager.getInstance().initialize();
        getInstance(); // warm up services
        System.out.println("[AppContext] Initialized.");
    }

    public static void shutdown() {
        DatabaseManager.getInstance().shutdown();
        FileStorageManager.getInstance().shutdown();
        System.out.println("[AppContext] Shutdown complete.");
    }

    // Getters
    public MedicineDAO    getMedicineDAO()    { return medicineDAO; }
    public UserDAO        getUserDAO()        { return userDAO; }
    public SupplierDAO    getSupplierDAO()    { return supplierDAO; }
    public InvoiceDAO     getInvoiceDAO()     { return invoiceDAO; }
    public PurchaseDAO    getPurchaseDAO()    { return purchaseDAO; }
    public ActivityLogDAO getActivityLogDAO() { return activityLogDAO; }
    public AlertDAO       getAlertDAO()       { return alertDAO; }
    public AnalyticsDAO   getAnalyticsDAO()   { return analyticsDAO; }

    public AuthService     getAuthService()     { return authService; }
    public MedicineService getMedicineService() { return medicineService; }
    public InvoiceService  getInvoiceService()  { return invoiceService; }
    public PurchaseService getPurchaseService() { return purchaseService; }
    public UserService     getUserService()     { return userService; }
    public SupplierService getSupplierService() { return supplierService; }
}
