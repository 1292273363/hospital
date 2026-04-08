const app = getApp();

Page({
  data: {
    userInfo: {},
    displayName: '医生',
    doctorTitle: '整形外科 | 主任医师',
    licenseNo: '11010**********',
    stats: {
      years: '15年',
      surgeries: '1200+',
      specialty: '面部整形',
      satisfaction: '98.5%'
    }
  },

  onShow() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().refresh();
    }
    const userInfo = wx.getStorageSync('userInfo') || {};
    const name = userInfo.nickName || userInfo.doctorName || '医生';
    const displayName = /医生$/.test(name) ? name : `${name}医生`;
    this.setData({
      userInfo,
      displayName
    });
  },

  goChat() {
    wx.navigateTo({ url: '/pages/doctor/chat' });
  },

  onAccountSettings() {
    wx.showToast({ title: '账号设置即将开放', icon: 'none' });
  },

  onSchedule() {
    wx.showToast({ title: '排班功能即将开放', icon: 'none' });
  },

  onReviews() {
    wx.showToast({ title: '评价功能即将开放', icon: 'none' });
  },

  onNotifySettings() {
    wx.showToast({ title: '通知设置即将开放', icon: 'none' });
  },

  onHelp() {
    wx.showToast({ title: '感谢您的反馈', icon: 'none' });
  },

  onAbout() {
    wx.showModal({
      title: '关于我们',
      content: '整形医院服务系统 医生端 v1.0.0',
      showCancel: false
    });
  },

  logout() {
    wx.showModal({
      title: '确认退出',
      content: '确定要退出当前登录账号吗？',
      confirmColor: '#ff4d4f',
      success: (res) => {
        if (!res.confirm) return;
        wx.removeStorageSync('token');
        wx.removeStorageSync('userInfo');
        wx.removeStorageSync('userRole');
        app.globalData.token = null;
        app.globalData.userInfo = null;
        wx.reLaunch({ url: '/pages/login/login' });
      }
    });
  }
});
