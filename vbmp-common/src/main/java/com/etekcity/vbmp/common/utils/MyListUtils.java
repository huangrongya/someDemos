package com.etekcity.vbmp.common.utils;

import com.etekcity.vbmp.common.comm.dao.model.AccountDevice;
import com.etekcity.vbmp.common.comm.dto.inner.Device;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.*;

public class MyListUtils {

    public static List<AccountDevice> removeDuplicate(List<AccountDevice> list) {
        List<AccountDevice> accountDeviceList = new ArrayList<>();
        if (list == null) {
            return accountDeviceList;
        }
        Set<String> set = new HashSet<>();
        for (int i = 0, size = list.size(); i < size; i++) {
            AccountDevice accountDevice = list.get(i);
            String key = accountDevice.getAccountId().concat("-").concat(accountDevice.getUuid());
            if (!set.contains(key)) {
                accountDeviceList.add(accountDevice);
                set.add(key);
            }
        }
        return accountDeviceList;
    }

    public static List<AccountDevice> addAccountDevice(List<String> accountIDList, String uuid) {
        List<AccountDevice> accountDeviceList = new ArrayList<>();
        if (accountIDList == null || !StringUtils.hasLength(uuid)) {
            return accountDeviceList;
        }
        for (int i = 0, size = accountIDList.size(); i < size; i++) {
            String accountID = accountIDList.get(i);
            AccountDevice accountDevice = new AccountDevice();
            accountDevice.setAccountId(accountID);
            accountDevice.setUuid(uuid);
            accountDeviceList.add(accountDevice);
        }
        return accountDeviceList;
    }

    public static List<String> getAccountDevice(List<AccountDevice> accountDevices, String uuid) {
        List<String> accountIDList = new ArrayList<>();
        if (accountDevices == null) {
            return accountIDList;
        }
        AccountDevice accountDevice;
        for (int i = 0, size = accountDevices.size(); i < size; i++) {
            accountDevice = accountDevices.get(i);
            //accountId为空,跳过
            if (!StringUtils.hasText(accountDevice.getAccountId())) {
                continue;
            }
            //uuid不为空.筛选
            if (StringUtils.hasLength(uuid) && uuid.equals(accountDevice.getUuid())) {
                accountIDList.add(accountDevice.getAccountId());
            } else if (!StringUtils.hasLength(uuid)) {
                accountIDList.add(accountDevice.getAccountId());
            }
        }
        return accountIDList;
    }

    public static List<AccountDevice> filterAccountDeviceByUuid(List<AccountDevice> accountDevices, String uuid) {
        List<AccountDevice> accountIDList = new ArrayList<>();
        if (accountDevices == null) {
            return accountIDList;
        }
        AccountDevice accountDevice;
        for (int i = 0, size = accountDevices.size(); i < size; i++) {
            accountDevice = accountDevices.get(i);
            if (StringUtils.hasLength(uuid) && !uuid.equals(accountDevice.getUuid())) {
                accountIDList.add(accountDevice);
            } else if (!StringUtils.hasLength(uuid)) {
                //accountIDList.add(accountDevice);
            }
        }
        return accountIDList;
    }

    /**
     * @param accountDevices
     * @param accountID
     * @return
     */
    public static List<AccountDevice> filterAccountDevicesByAccountID(List<AccountDevice> accountDevices, String accountID) {
        List<AccountDevice> accountIDList = new ArrayList<>();
        if (accountDevices == null) {
            return accountIDList;
        }
        AccountDevice accountDevice;
        for (int i = 0, size = accountDevices.size(); i < size; i++) {
            accountDevice = accountDevices.get(i);
            if (StringUtils.hasLength(accountID) && !accountID.equals(accountDevice.getAccountId())) {
                accountIDList.add(accountDevice);
            } else if (!StringUtils.hasLength(accountID)) {
                accountIDList.add(accountDevice);
            }
        }
        return accountIDList;
    }

    public static boolean findAndRemoveElement(List<String> list, String element) {
        boolean ok = true;
        if (list == null) {
            return ok;
        }
        if (!list.contains(element)) {
            return ok;
        }
        return list.remove(element);
    }

    public static List<Device> copyToList(List<Device> src, List<Device> dest) {
        Assert.notNull(dest, "不能为空");
        if (src == null) {
            src = new ArrayList<>();
        }
        List<Device> srcCopy = new ArrayList<>();
        srcCopy.addAll(Arrays.asList(new Device[src.size()]));
        Collections.copy(srcCopy, src);
        dest.addAll(srcCopy);
        return dest;
    }

    public static Float sumList(List<Float> list) {
        if (list == null) {
            list = new ArrayList<>();
        }
        Float total = list.stream().reduce(0f, Float::sum);
        return total;
    }

}
