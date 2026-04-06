// pages/index/index.js — 患者端主页
const app = getApp();
const { request } = require('../../utils/request');

// 本地兜底公告（用于后端未部署/接口异常时展示）
const fallbackNotices = [
  { id: 1, title: '五一节假日门诊安排调整通知', time: '2023-04-28' },
  { id: 2, title: '我院新增整形美容专家门诊', time: '2023-04-15' },
  { id: 3, title: '夏季整形优惠活动开始啦', time: '2023-04-02' }
];

Page({
  data: {
    userInfo: {},
    userDisplayId: '—',
    visitStatus: {
      doctorName: '—',
      visitType: '—',
      lastUpdate: '—'
    },
    visitReminder: {
      content: '请于复诊时间到院复查，具体以科室通知为准。',
      time: ''
    },
    quickMenus: [
      { id: 'upload', icon: '☁️', label: '上传诊断档案' },
      { id: 'consult', icon: '💬', label: '咨询医生' },
      { id: 'profile', icon: '👤', label: '个人中心' },
      { id: 'records', icon: '📜', label: '就诊记录' }
    ],
    notices: fallbackNotices
  },

  onLoad() {
    this.loadUserInfo();
    this.loadNotices();
  },

  onShow() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().refresh();
    }
    this.loadUserInfo();
    this.loadNotices();
  },

  async loadNotices() {
    try {
      const result = await request({
        url: '/api/notice/active',
        method: 'GET',
        showLoading: false
      });
      const list = (result && result.data) ? result.data : [];
      if (Array.isArray(list)) {
        this.setData({ notices: list });
      }
    } catch (e) {
      // 保持兜底公告
    }
  },

  loadUserInfo() {
    const userInfo = wx.getStorageSync('userInfo') || {};
    const id = userInfo.id;
    const userDisplayId = id != null ? String(id).padStart(8, '0') : '—';
    const now = new Date();
    const timeStr = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
    this.setData({
      userInfo,
      userDisplayId,
      'visitStatus.doctorName': '李医生',
      'visitStatus.visitType': '面部整形咨询',
      'visitStatus.lastUpdate': '2023-05-15',
      'visitReminder.time': timeStr
    });
  },

  onMenuTap(e) {
    const id = e.currentTarget.dataset.id;
    switch (id) {
      case 'upload':
        wx.navigateTo({ url: '/pages/upload/index' });
        break;
      case 'consult':
        wx.navigateTo({ url: '/pages/consult/index' });
        break;
      case 'profile':
        wx.switchTab({ url: '/pages/center/index' });
        break;
      case 'records':
        wx.navigateTo({ url: '/pages/patient/index' });
        break;
      default:
        wx.showToast({ title: '功能开发中', icon: 'none' });
    }
  },

  onLogout() {
    wx.showModal({
      title: '确认退出',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          wx.removeStorageSync('token');
          wx.removeStorageSync('userInfo');
          wx.removeStorageSync('userRole');
          app.globalData.token = null;
          app.globalData.userInfo = null;
          wx.reLaunch({ url: '/pages/login/login' });
        }
      }
    });
  }
});
