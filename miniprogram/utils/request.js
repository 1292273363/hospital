// utils/request.js - 封装网络请求
const app = getApp();

/**
 * 封装wx.request，统一处理token和错误
 * @param {Object} options - 请求配置
 */
const request = (options) => {
  const { url, method = 'GET', data = {}, showLoading = true } = options;

  if (showLoading) {
    wx.showLoading({ title: '加载中...', mask: true });
  }

  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync('token');
    const header = {
      'Content-Type': 'application/json',
    };
    if (token) {
      header['Authorization'] = 'Bearer ' + token;
    }

    wx.request({
      url: app.globalData.baseUrl + url,
      method,
      data,
      header,
      success(res) {
        if (showLoading) wx.hideLoading();
        if (res.statusCode === 200) {
          const result = res.data;
          if (result.code === 200) {
            resolve(result);
          } else if (result.code === 401) {
            // token过期，跳转登录
            wx.removeStorageSync('token');
            wx.removeStorageSync('userInfo');
            wx.removeStorageSync('userRole');
            wx.reLaunch({ url: '/pages/login/login' });
            reject(result);
          } else {
            wx.showToast({ title: result.message || '请求失败', icon: 'none' });
            reject(result);
          }
        } else {
          wx.showToast({ title: '网络错误：' + res.statusCode, icon: 'none' });
          reject(res);
        }
      },
      fail(err) {
        if (showLoading) wx.hideLoading();
        wx.showToast({ title: '网络连接失败', icon: 'none' });
        reject(err);
      }
    });
  });
};

module.exports = { request };

