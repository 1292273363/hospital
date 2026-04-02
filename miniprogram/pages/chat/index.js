const app = getApp();
const { request } = require('../../utils/request');

Page({
  data: {
    currentPatient: null,
    historyList: []
  },

  onShow() {
    const currentPatient = wx.getStorageSync('currentPatient') || null;
    this.setData({ currentPatient, historyList: [] });
    if (currentPatient) {
      this.loadHistory(currentPatient.id);
    }
  },

  async loadHistory(patientRecordId) {
    try {
      const result = await request({
        url: `/api/consult/history?patientRecordId=${patientRecordId}`,
        method: 'GET',
        showLoading: false
      });
      const historyList = (result.data || []).map(item => ({
        ...item,
        scarImageUrl: item.scarImageUrl && item.scarImageUrl.startsWith('/')
          ? `${app.globalData.baseUrl}${item.scarImageUrl}`
          : item.scarImageUrl
      }));
      this.setData({ historyList });
    } catch (error) {
      console.error('加载沟通记录失败', error);
    }
  },

  goSelectPatient() {
    wx.switchTab({ url: '/pages/center/index' });
  },

  goConsult() {
    wx.navigateTo({ url: '/pages/consult/index' });
  }
});
