// pages/index/index.js
const app = getApp();

Page({
  data: {
    userInfo: {},
    quickMenus: [
      { id: 1, icon: '🏥', label: '在线挂号' },
      { id: 2, icon: '📋', label: '检查报告' },
      { id: 3, icon: '💊', label: '用药提醒' },
      { id: 4, icon: '📞', label: '在线问诊' },
      { id: 5, icon: '🧾', label: '缴费记录' },
      { id: 6, icon: '📅', label: '我的预约' },
      { id: 7, icon: '🏃', label: '健康档案' },
      { id: 8, icon: '⚙️', label: '个人设置' }
    ],
    notices: [
      { id: 1, title: '门诊时间调整通知：元旦假期正常出诊', time: '今天' },
      { id: 2, title: '皮肤科专家门诊本周六增加夜间号源，请提前预约', time: '昨天' },
      { id: 3, title: '医院停车场升级改造，部分区域暂停使用', time: '3天前' }
    ]
  },

  onLoad() {
    this.loadUserInfo();
  },

  onShow() {
    this.loadUserInfo();
  },

  loadUserInfo() {
    const userInfo = wx.getStorageSync('userInfo') || {};
    this.setData({ userInfo });
  },

  onMenuTap(e) {
    const id = e.currentTarget.dataset.id;
    if (id === 4) {
      wx.navigateTo({ url: '/pages/consult/index' });
      return;
    }
    if (id === 8) {
      wx.switchTab({ url: '/pages/center/index' });
      return;
    }
    wx.showToast({ title: '功能开发中', icon: 'none' });
  },

  onLogout() {
    wx.showModal({
      title: '确认退出',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          wx.removeStorageSync('token');
          wx.removeStorageSync('userInfo');
          app.globalData.token = null;
          app.globalData.userInfo = null;
          wx.reLaunch({ url: '/pages/login/login' });
        }
      }
    });
  }
});

