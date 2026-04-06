Page({
  data: {
    patientId: null,
    patientName: '',
    records: []
  },

  onLoad(options) {
    const id = Number(options.id || 0);
    const patients = [
      { id: 1, name: '张三' },
      { id: 2, name: '李四' },
      { id: 3, name: '王五' },
      { id: 4, name: '赵六' },
      { id: 5, name: '孙七' }
    ];
    const patient = patients.find((p) => p.id === id) || patients[0] || { id, name: '' };
    const baseName = patient.name || '该患者';

    // 模拟：同一患者的两条上传档案
    const mockRecords = [
      {
        id: 1,
        name: `${baseName} 初诊面诊记录.pdf`,
        date: '2024-01-10',
        type: 'file',
        remark: '初次面诊病历及手术评估'
      },
      {
        id: 2,
        name: `${baseName} 术前检查报告.jpg`,
        date: '2024-01-12',
        type: 'image',
        remark: '化验单及影像资料'
      }
    ];

    this.setData({
      patientId: patient.id,
      patientName: patient.name,
      records: mockRecords
    });
  },

  onPreview(e) {
    const recordId = e.currentTarget.dataset.id;
    const { patientId } = this.data;
    wx.navigateTo({
      url: `/pages/doctor/record-detail?pid=${patientId}&rid=${recordId}`
    });
  }
});

